package com.slayerloadout;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@Slf4j
@PluginDescriptor(
	name = "Slayer Loadout",
	description = "Shows the best gear you own (Melee, Ranged, Magic) for your current slayer task",
	tags = {"slayer", "gear", "bis", "loadout", "combat", "bank", "task"}
)
public class SlayerLoadoutPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private SlayerLoadoutConfig config;

	@Inject
	private BisDataService bisData;

	private final SlayerTaskTracker tracker = new SlayerTaskTracker();

	private SlayerLoadoutPanel panel;
	private NavigationButton navButton;

	// Snapshot of owned items, rebuilt on the client thread and read on the EDT.
	private volatile OwnedItemIndex owned = new OwnedItemIndex();

	// Transient in-panel override: a monster the user typed to preview. Takes
	// precedence over the auto-detected task until they hit "Back to my task".
	private volatile String panelOverride;

	// Player's (boosted) combat levels for the DPS calculator.
	private volatile PlayerStats playerStats = PlayerStats.maxed();

	@Override
	protected void startUp()
	{
		bisData.load();

		// Restore the last detected task so the panel is useful immediately.
		tracker.setCurrentTask(config.lastTask());

		panel = new SlayerLoadoutPanel(new SlayerLoadoutPanel.TaskOverrideListener()
		{
			@Override
			public void onManualTask(String monster)
			{
				panelOverride = monster;
				rebuild();
			}

			@Override
			public void onClearOverride()
			{
				panelOverride = null;
				rebuild();
			}
		});

		navButton = NavigationButton.builder()
			.tooltip("Slayer Loadout")
			.icon(createIcon())
			.priority(7)
			.panel(panel)
			.build();

		panel.setSuggestions(bisData.getSuggestionNames());

		clientToolbar.addNavigation(navButton);

		clientThread.invokeLater(() ->
		{
			refreshStats();
			refreshOwned();
			rebuild();
		});
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		panel = null;
		navButton = null;
		owned = new OwnedItemIndex();
	}

	@Provides
	SlayerLoadoutConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SlayerLoadoutConfig.class);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		final ChatMessageType type = event.getType();
		if (type != ChatMessageType.GAMEMESSAGE && type != ChatMessageType.SPAM)
		{
			return;
		}

		if (tracker.parse(event.getMessage()))
		{
			final String task = tracker.getCurrentTask();
			configManager.setConfiguration(SlayerLoadoutConfig.GROUP, "lastTask", task == null ? "" : task);
			rebuild();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		final int id = event.getContainerId();
		if (id == InventoryID.BANK.getId()
			|| id == InventoryID.INVENTORY.getId()
			|| id == InventoryID.EQUIPMENT.getId())
		{
			refreshOwned();
			rebuild();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			refreshStats();
			refreshOwned();
			rebuild();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		refreshStats();
		rebuild();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!SlayerLoadoutConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		clientThread.invokeLater(() ->
		{
			refreshStats();
			refreshOwned();
			rebuild();
		});
	}

	/**
	 * Rebuild the {@link #owned} snapshot from the bank, inventory and worn
	 * equipment containers. Must run on the client thread.
	 */
	private void refreshOwned()
	{
		final OwnedItemIndex index = new OwnedItemIndex();

		if (config.includeBank())
		{
			addContainer(index, InventoryID.BANK);
		}
		if (config.includeInventory())
		{
			addContainer(index, InventoryID.INVENTORY);
		}
		if (config.includeEquipment())
		{
			addContainer(index, InventoryID.EQUIPMENT);
		}

		owned = index;
	}

	/** Read the player's boosted combat levels for the DPS calculator. Client thread. */
	private void refreshStats()
	{
		playerStats = new PlayerStats(
			client.getBoostedSkillLevel(Skill.ATTACK),
			client.getBoostedSkillLevel(Skill.STRENGTH),
			client.getBoostedSkillLevel(Skill.DEFENCE),
			client.getBoostedSkillLevel(Skill.RANGED),
			client.getBoostedSkillLevel(Skill.MAGIC),
			client.getBoostedSkillLevel(Skill.HITPOINTS));
	}

	private void addContainer(OwnedItemIndex index, InventoryID inventoryID)
	{
		final ItemContainer container = client.getItemContainer(inventoryID);
		if (container == null)
		{
			return;
		}

		for (Item item : container.getItems())
		{
			if (item == null)
			{
				continue;
			}
			final int rawId = item.getId();
			if (rawId <= 0 || item.getQuantity() <= 0)
			{
				// Skip empty slots and bank placeholders (quantity 0).
				continue;
			}

			// Resolve noted items and placeholders back to the real item.
			final int canonical = itemManager.canonicalize(rawId);
			final String name = itemManager.getItemComposition(canonical).getName();

			final ItemStats stats = itemManager.getItemStats(canonical);
			GearSlot slot = null;
			if (stats != null && stats.isEquipable() && stats.getEquipment() != null)
			{
				final ItemEquipmentStats e = stats.getEquipment();
				slot = GearSlot.fromEquipmentSlot(e.getSlot());
				if (slot != null)
				{
					final int meleeAtk = Math.max(e.getAstab(), Math.max(e.getAslash(), e.getAcrush()));
					final int totalDef = e.getDstab() + e.getDslash() + e.getDcrush() + e.getDmagic() + e.getDrange();
					final int magicDmg = (int) e.getMdmg();

					final int meleeScore = e.getStr() * 10 + meleeAtk + totalDef / 10;
					final int rangedScore = e.getRstr() * 10 + e.getArange() + totalDef / 10;
					final int magicScore = magicDmg * 10 + e.getAmagic() + totalDef / 10;

					// For weapons, classify the intended style by the dominant attack bonus,
					// so a melee weapon never gets suggested for a Ranged/Magic loadout.
					AttackStyle weaponStyle = null;
					if (slot == GearSlot.WEAPON)
					{
						final int rangeAtk = e.getArange();
						final int magicAtk = e.getAmagic();
						if (magicAtk > meleeAtk && magicAtk >= rangeAtk)
						{
							weaponStyle = AttackStyle.MAGIC;
						}
						else if (rangeAtk > meleeAtk && rangeAtk >= magicAtk)
						{
							weaponStyle = AttackStyle.RANGED;
						}
						else
						{
							weaponStyle = AttackStyle.MELEE;
						}
					}

					index.addEquippable(name, canonical, slot, meleeScore, rangedScore, magicScore, e.isTwoHanded(),
						weaponStyle, e.getStr(), e.getAstab(), e.getAslash(), e.getAcrush(), e);
				}
			}

			if (slot == null)
			{
				index.add(name, canonical);
			}
		}
	}

	private boolean hasPanelOverride()
	{
		return panelOverride != null && !panelOverride.trim().isEmpty();
	}

	private String resolveTaskName()
	{
		// 1) in-panel "preview" override, 2) persistent config override, 3) detected task.
		if (hasPanelOverride())
		{
			return panelOverride.trim();
		}
		final String manual = config.manualTask();
		if (manual != null && !manual.trim().isEmpty())
		{
			return manual.trim();
		}
		return tracker.getCurrentTask();
	}

	private void rebuild()
	{
		final String task = resolveTaskName();
		final boolean override = hasPanelOverride();
		final boolean preferBroadBolts = config.preferBroadBolts();
		final boolean assumePrayers = config.assumePrayers();
		final PlayerStats stats = playerStats;
		final MonsterLoadout loadout = task == null ? null : bisData.find(task);
		final OwnedItemIndex snapshot = owned;

		SwingUtilities.invokeLater(() ->
		{
			if (panel != null)
			{
				panel.update(task, override, preferBroadBolts, assumePrayers, stats, loadout, snapshot, itemManager);
			}
		});
	}

	/** Build a small navigation-button icon at runtime (no binary resource needed). */
	private static BufferedImage createIcon()
	{
		final int size = 24;
		final BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(0xE0, 0x90, 0x20));
		g.fillRoundRect(1, 1, size - 2, size - 2, 6, 6);
		g.setColor(new Color(0x2B, 0x2B, 0x2B));
		g.setFont(g.getFont().deriveFont(java.awt.Font.BOLD, 14f));
		final String text = "S";
		final int tw = g.getFontMetrics().stringWidth(text);
		g.drawString(text, (size - tw) / 2, 17);
		g.dispose();
		return image;
	}
}
