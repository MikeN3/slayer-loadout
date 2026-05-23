package com.slayerloadout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * Side panel that shows the current slayer task and, for each combat style, the
 * best gear the player actually owns (bank + inventory + equipment).
 */
class SlayerLoadoutPanel extends PluginPanel
{
	/** Lets the panel ask the plugin to recompute for a typed monster, or revert. */
	interface TaskOverrideListener
	{
		void onManualTask(String monster);

		void onClearOverride();
	}

	private final JPanel content = new JPanel();
	private final JLabel titleLabel = new JLabel();
	private final JLabel statusLabel = new JLabel();
	private final JTextField overrideField = new JTextField();
	private final JButton backButton = new JButton("Back to my task");
	private final TaskOverrideListener listener;

	// Autocomplete suggestion popup for the override field.
	private final DefaultListModel<String> suggestionModel = new DefaultListModel<>();
	private final JList<String> suggestionList = new JList<>(suggestionModel);
	private JWindow suggestionWindow;
	private List<String> suggestions = Collections.emptyList();
	private boolean suppressSuggest = false;

	// Settings/state mirrored from the plugin on each update.
	private boolean preferBroadBolts = true;
	private boolean assumePrayers = true;
	private boolean preferBlowpipe = false;
	private PlayerStats playerStats = PlayerStats.maxed();

	SlayerLoadoutPanel(TaskOverrideListener listener)
	{
		super(false);
		this.listener = listener;

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		titleLabel.setFont(FontManager.getRunescapeBoldFont());
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

		content.setLayout(new GridBagLayout());
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// --- Manual override controls ---
		final JLabel hint = new JLabel("Preview another monster:");
		hint.setFont(FontManager.getRunescapeSmallFont());
		hint.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		hint.setAlignmentX(Component.LEFT_ALIGNMENT);

		overrideField.setToolTipText("Type a slayer monster (e.g. gargoyles) and press Enter");
		overrideField.setAlignmentX(Component.LEFT_ALIGNMENT);
		overrideField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		overrideField.addActionListener(e -> applyFromField());
		setupAutocomplete();

		backButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		backButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		backButton.setFocusPainted(false);
		backButton.setVisible(false);
		backButton.addActionListener(e ->
		{
			overrideField.setText("");
			listener.onClearOverride();
		});

		final JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
		controls.setBackground(ColorScheme.DARK_GRAY_COLOR);
		controls.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
		controls.add(hint);
		controls.add(Box.createVerticalStrut(4));
		controls.add(overrideField);
		controls.add(Box.createVerticalStrut(4));
		controls.add(backButton);

		final JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);
		header.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
		header.add(titleLabel, BorderLayout.NORTH);
		header.add(statusLabel, BorderLayout.CENTER);
		header.add(controls, BorderLayout.SOUTH);

		// Keep the content stacked at the top (natural heights) inside a scroll pane
		// so a tall loadout (3 styles x many slots) scrolls instead of being clipped.
		final JPanel contentHolder = new JPanel(new BorderLayout());
		contentHolder.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentHolder.add(content, BorderLayout.NORTH);

		final JScrollPane scrollPane = new JScrollPane(contentHolder,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		add(header, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);

		showNoTask();
	}

	/** Render an empty state (no detected task). */
	void showNoTask()
	{
		titleLabel.setText("No slayer task");
		titleLabel.setForeground(Color.WHITE);
		statusLabel.setText("<html>Check your task at a Slayer Master or on a gem, "
			+ "or type a monster above to preview its loadout.</html>");
		content.removeAll();
		content.revalidate();
		content.repaint();
	}

	/**
	 * Update the panel for the given task.
	 *
	 * @param taskName         detected/overridden task name (may be null)
	 * @param override         true if this is a manually-entered monster (not auto-detected)
	 * @param preferBroadBolts setting: use broad bolts for crossbow ammo when owned
	 * @param assumePrayers    setting: include offensive prayers in the DPS estimate
	 * @param preferBlowpipe   setting: force a blowpipe as the ranged weapon when owned
	 * @param playerStats      the player's (boosted) combat levels
	 * @param loadout          dataset entry for the task (may be null if unknown)
	 * @param owned            snapshot of items the player owns
	 * @param itemManager      used to fetch item icons
	 */
	void update(String taskName, boolean override, boolean preferBroadBolts, boolean assumePrayers,
		boolean preferBlowpipe, PlayerStats playerStats, MonsterLoadout loadout, OwnedItemIndex owned,
		ItemManager itemManager)
	{
		this.preferBroadBolts = preferBroadBolts;
		this.assumePrayers = assumePrayers;
		this.preferBlowpipe = preferBlowpipe;
		this.playerStats = playerStats != null ? playerStats : PlayerStats.maxed();

		// The "back to my task" button is only relevant while previewing an override.
		backButton.setVisible(override);

		if (taskName == null || taskName.trim().isEmpty())
		{
			showNoTask();
			revalidate();
			repaint();
			return;
		}

		titleLabel.setText((override ? "Preview: " : "Task: ") + capitalize(taskName));
		titleLabel.setForeground(override ? ColorScheme.BRAND_ORANGE : Color.WHITE);
		content.removeAll();

		if (loadout == null)
		{
			statusLabel.setText("<html>No gear data for <b>" + escape(taskName)
				+ "</b>. Check the spelling, or add it to <i>bis-data.json</i>.</html>");
			content.revalidate();
			content.repaint();
			revalidate();
			repaint();
			return;
		}

		final int ownedCount = owned == null ? 0 : owned.size();
		final StringBuilder status = new StringBuilder("<html>");
		status.append("Showing the best set you own across bank, inventory &amp; equipment.");
		if (ownedCount == 0)
		{
			status.append("<br><span style='color:#e0a030'>No items detected yet - open your bank once.</span>");
		}
		if (loadout.getNotes() != null && !loadout.getNotes().isEmpty())
		{
			status.append("<br><br><b>Note:</b> ").append(escape(loadout.getNotes()));
		}
		status.append("</html>");
		statusLabel.setText(status.toString());

		final AttackStyle recommended = loadout.getRecommendedStyle();

		final GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 1;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.anchor = GridBagConstraints.NORTH;
		gc.insets = new Insets(0, 0, 8, 0);

		// Boss tasks: suggest a defence-reducing special-attack weapon you own.
		if (loadout.isBoss() && owned != null)
		{
			final OwnedItemIndex.OwnedItem spec = bestSpecWeapon(owned);
			if (spec != null)
			{
				content.add(buildSpecSection(spec, itemManager), gc);
				gc.gridy++;
			}
		}

		for (AttackStyle style : AttackStyle.values())
		{
			if (!loadout.hasStyle(style))
			{
				continue;
			}
			content.add(buildStyleSection(style, style == recommended, loadout, owned, itemManager), gc);
			gc.gridy++;
		}

		content.revalidate();
		content.repaint();
		revalidate();
		repaint();
	}

	/** Ammunition families, used to match ammo to the chosen ranged weapon. */
	private enum AmmoType
	{
		BOLTS, ARROWS, DARTS, JAVELINS, NONE, UNKNOWN
	}

	private static final Color FALLBACK_COLOR = new Color(120, 170, 255);
	private static final Color EMPTY_COLOR = new Color(150, 90, 90);
	private static final Color INFO_COLOR = new Color(150, 150, 150);
	private static final Color SET_COLOR = new Color(120, 200, 120);
	private static final Color SPELL_COLOR = new Color(190, 150, 230);

	private static final class Pick
	{
		final OwnedItemIndex.OwnedItem item;
		final boolean curated;
		final boolean setBonus;
		final String placeholder;

		Pick(OwnedItemIndex.OwnedItem item, boolean curated, boolean setBonus, String placeholder)
		{
			this.item = item;
			this.curated = curated;
			this.setBonus = setBonus;
			this.placeholder = placeholder;
		}
	}

	private JPanel buildStyleSection(AttackStyle style, boolean recommended, MonsterLoadout loadout,
		OwnedItemIndex owned, ItemManager itemManager)
	{
		final JPanel section = new JPanel(new BorderLayout());
		section.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		section.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
			BorderFactory.createEmptyBorder(6, 6, 6, 6)));

		final JPanel rows = new JPanel(new GridBagLayout());
		rows.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0;
		c.insets = new Insets(1, 0, 1, 4);

		final Map<String, List<String>> styleMap = loadout.getStyle(style);
		final Map<GearSlot, OwnedItemIndex.OwnedItem> setBonusMap = computeSetBonus(style, loadout, owned);

		// Phase 1: heuristic pick per slot.
		final Map<GearSlot, Pick> picks = new EnumMap<>(GearSlot.class);
		OwnedItemIndex.OwnedItem chosenWeapon = null;
		for (GearSlot slot : GearSlot.values())
		{
			if (!styleMap.containsKey(slot.name()))
			{
				continue;
			}
			final Pick pick = selectSlot(slot, style, loadout, owned, setBonusMap, chosenWeapon);
			picks.put(slot, pick);
			if (slot == GearSlot.WEAPON && pick.item != null)
			{
				chosenWeapon = pick.item;
			}
		}

		// Phase 2: with monster combat stats, pick the weapon by computed DPS and show the number.
		Double dps = null;
		final MonsterStats mstats = loadout.getCombat();
		if (mstats != null && owned != null)
		{
			final Map<GearSlot, OwnedItemIndex.OwnedItem> calcLoadout = new EnumMap<>(GearSlot.class);
			for (Map.Entry<GearSlot, Pick> e : picks.entrySet())
			{
				if (e.getKey() != GearSlot.WEAPON && e.getValue().item != null)
				{
					calcLoadout.put(e.getKey(), e.getValue().item);
				}
			}

			OwnedItemIndex.OwnedItem bestWeapon = null;
			double bestDps = -1;
			final OwnedItemIndex.OwnedItem forcedBlowpipe =
				(style == AttackStyle.RANGED && preferBlowpipe) ? findBlowpipe(owned) : null;
			if (forcedBlowpipe != null)
			{
				// User opted to always use the blowpipe; report its DPS but don't let
				// the loop pick anything else.
				calcLoadout.put(GearSlot.WEAPON, forcedBlowpipe);
				bestWeapon = forcedBlowpipe;
				bestDps = DpsCalculator.computeDps(style, playerStats, assumePrayers, calcLoadout,
					mstats, loadout.getAttributes());
			}
			else
			{
				for (OwnedItemIndex.OwnedItem w : owned.weaponsForStyle(style))
				{
					calcLoadout.put(GearSlot.WEAPON, w);
					final double d = DpsCalculator.computeDps(style, playerStats, assumePrayers, calcLoadout,
						mstats, loadout.getAttributes());
					if (d > bestDps)
					{
						bestDps = d;
						bestWeapon = w;
					}
				}
			}

			if (bestWeapon != null)
			{
				picks.put(GearSlot.WEAPON, new Pick(bestWeapon, true, false, "- none owned"));
				chosenWeapon = bestWeapon;
				dps = bestDps;
				// The weapon changed, so re-evaluate the slots that depend on it.
				if (picks.containsKey(GearSlot.SHIELD))
				{
					picks.put(GearSlot.SHIELD, selectSlot(GearSlot.SHIELD, style, loadout, owned, setBonusMap, chosenWeapon));
				}
				if (picks.containsKey(GearSlot.AMMO))
				{
					picks.put(GearSlot.AMMO, selectSlot(GearSlot.AMMO, style, loadout, owned, setBonusMap, chosenWeapon));
				}
			}
			else if (chosenWeapon != null)
			{
				calcLoadout.put(GearSlot.WEAPON, chosenWeapon);
				dps = DpsCalculator.computeDps(style, playerStats, assumePrayers, calcLoadout,
					mstats, loadout.getAttributes());
			}

			// Void / Elite Void: try the complete void set (when owned) as a candidate
			// and adopt it if its set bonus beats the best mix-and-match armour. The DPS
			// calculator weighs void's bonus against e.g. the Slayer helmet's, so void is
			// only chosen when it genuinely wins.
			if (dps != null)
			{
				final Map<GearSlot, OwnedItemIndex.OwnedItem> voidPieces = voidSetFor(style, owned);
				if (voidPieces != null)
				{
					final Map<GearSlot, OwnedItemIndex.OwnedItem> voidLoadout = new EnumMap<>(GearSlot.class);
					for (Map.Entry<GearSlot, Pick> e : picks.entrySet())
					{
						if (e.getValue().item != null)
						{
							voidLoadout.put(e.getKey(), e.getValue().item);
						}
					}
					voidLoadout.putAll(voidPieces);
					final double voidDps = DpsCalculator.computeDps(style, playerStats, assumePrayers,
						voidLoadout, mstats, loadout.getAttributes());
					if (voidDps > dps)
					{
						dps = voidDps;
						for (Map.Entry<GearSlot, OwnedItemIndex.OwnedItem> e : voidPieces.entrySet())
						{
							picks.put(e.getKey(), new Pick(e.getValue(), false, true, "- none owned"));
						}
					}
				}
			}
		}

		// Header (with DPS estimate if available).
		final StringBuilder headText = new StringBuilder(style.getDisplayName());
		if (recommended)
		{
			headText.append("  (recommended)");
		}
		if (dps != null)
		{
			headText.append(String.format(Locale.ENGLISH, "  -  %.1f DPS", dps));
		}
		final JLabel head = new JLabel(headText.toString());
		head.setFont(FontManager.getRunescapeBoldFont());
		head.setForeground(recommended ? ColorScheme.BRAND_ORANGE : Color.WHITE);
		head.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
		section.add(head, BorderLayout.NORTH);

		// Render rows in slot order.
		for (GearSlot slot : GearSlot.values())
		{
			final Pick pk = picks.get(slot);
			if (pk == null)
			{
				continue;
			}
			addRow(rows, c, slot, pk.item, pk.curated, pk.setBonus, pk.placeholder, itemManager);
			c.gridy++;
		}

		// Magic: recommend the best spellbook spell (or note the powered-staff attack).
		if (style == AttackStyle.MAGIC)
		{
			final JLabel spellLabel = new JLabel("Spell");
			spellLabel.setFont(FontManager.getRunescapeSmallFont());
			spellLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			spellLabel.setPreferredSize(new Dimension(52, 32));
			spellLabel.setVerticalAlignment(SwingConstants.CENTER);
			c.gridx = 0;
			c.weightx = 0;
			rows.add(spellLabel, c);

			final JPanel spellCell = new JPanel();
			spellCell.setLayout(new BoxLayout(spellCell, BoxLayout.X_AXIS));
			spellCell.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			final JLabel spellVal = new JLabel(recommendedSpell(chosenWeapon, loadout));
			spellVal.setFont(FontManager.getRunescapeSmallFont());
			spellVal.setForeground(SPELL_COLOR);
			spellCell.add(spellVal);
			c.gridx = 1;
			c.weightx = 1;
			rows.add(spellCell, c);
			c.gridy++;
		}

		section.add(rows, BorderLayout.CENTER);
		section.setAlignmentX(Component.LEFT_ALIGNMENT);
		return section;
	}

	/** Heuristic pick for a single slot (set bonus, curated, weakness, fallback). */
	private Pick selectSlot(GearSlot slot, AttackStyle style, MonsterLoadout loadout, OwnedItemIndex owned,
		Map<GearSlot, OwnedItemIndex.OwnedItem> setBonusMap, OwnedItemIndex.OwnedItem chosenWeapon)
	{
		final Map<String, List<String>> styleMap = loadout.getStyle(style);
		OwnedItemIndex.OwnedItem chosen = null;
		boolean curated = false;
		boolean setBonus = false;
		String placeholder = "- none owned";

		final OwnedItemIndex.OwnedItem setPiece = setBonusMap.get(slot);
		if (setPiece != null)
		{
			chosen = setPiece;
			setBonus = true;
		}
		else if (slot == GearSlot.SHIELD && chosenWeapon != null && chosenWeapon.twoHanded)
		{
			placeholder = "- 2-handed weapon";
		}
		else if (slot == GearSlot.AMMO)
		{
			final AmmoType type = classifyWeapon(chosenWeapon == null ? null : chosenWeapon.name);
			if (type == AmmoType.NONE)
			{
				placeholder = "- no ammo needed";
			}
			else if (owned != null && type != AmmoType.UNKNOWN)
			{
				if (type == AmmoType.BOLTS)
				{
					// Boss tasks: enchanted dragon bolts (Ruby/Diamond) outdamage broad bolts.
					if (loadout.isBoss())
					{
						chosen = owned.match("Ruby dragon bolts (e)");
						if (chosen == null)
						{
							chosen = owned.match("Diamond dragon bolts (e)");
						}
					}
					// Otherwise broad bolts hit almost every slayer monster.
					if (chosen == null && preferBroadBolts)
					{
						chosen = owned.match("Amethyst broad bolts");
						if (chosen == null)
						{
							chosen = owned.match("Broad bolts");
						}
					}
				}
				if (chosen == null)
				{
					chosen = bestOwnedAmmo(owned, type, style);
				}
			}
		}
		else
		{
			// Blowpipe override (ranged weapon): when enabled and a blowpipe is owned,
			// force it regardless of the curated list or DPS pick.
			if (slot == GearSlot.WEAPON && style == AttackStyle.RANGED && preferBlowpipe && owned != null)
			{
				final OwnedItemIndex.OwnedItem bp = findBlowpipe(owned);
				if (bp != null)
				{
					return new Pick(bp, true, false, "- none owned");
				}
			}
			final List<String> candidates = styleMap.get(slot.name());
			if (owned != null && candidates != null)
			{
				for (String candidate : candidates)
				{
					final OwnedItemIndex.OwnedItem oi = owned.match(candidate);
					if (oi != null)
					{
						chosen = oi;
						curated = true;
						break;
					}
				}
			}
			if (slot == GearSlot.WEAPON && style == AttackStyle.MELEE && owned != null)
			{
				final String weakness = loadout.getMeleeWeakness();
				if (weakness != null && (chosen == null || isGenericMelee(chosen.name)))
				{
					final OwnedItemIndex.OwnedItem weakPick = owned.bestMeleeWeaponForWeakness(weakness);
					if (weakPick != null)
					{
						chosen = weakPick;
						curated = true;
					}
				}
			}
			if (chosen == null && owned != null)
			{
				chosen = owned.bestInSlot(slot, style);
			}
		}
		return new Pick(chosen, curated, setBonus, placeholder);
	}

	private void addRow(JPanel rows, GridBagConstraints c, GearSlot slot,
		OwnedItemIndex.OwnedItem chosen, boolean curated, boolean setBonus, String placeholder, ItemManager itemManager)
	{
		final JLabel slotLabel = new JLabel(slot.getDisplayName());
		slotLabel.setFont(FontManager.getRunescapeSmallFont());
		slotLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		slotLabel.setPreferredSize(new Dimension(52, 32));
		slotLabel.setVerticalAlignment(SwingConstants.CENTER);

		c.gridx = 0;
		c.weightx = 0;
		rows.add(slotLabel, c);

		final JPanel cell = new JPanel();
		cell.setLayout(new BoxLayout(cell, BoxLayout.X_AXIS));
		cell.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		if (chosen != null)
		{
			final JLabel icon = new JLabel();
			icon.setPreferredSize(new Dimension(36, 32));
			final AsyncBufferedImage image = itemManager.getImage(chosen.id);
			if (image != null)
			{
				image.addTo(icon);
			}
			cell.add(icon);

			final JLabel name = new JLabel(chosen.name);
			name.setFont(FontManager.getRunescapeSmallFont());
			// Green = part of a set bonus; white = curated best-in-slot; blue = best owned alternative.
			name.setForeground(setBonus ? SET_COLOR : curated ? Color.WHITE : FALLBACK_COLOR);
			cell.add(name);
		}
		else
		{
			final JLabel none = new JLabel(placeholder);
			none.setFont(FontManager.getRunescapeSmallFont());
			none.setForeground(placeholder.contains("none owned") ? EMPTY_COLOR : INFO_COLOR);
			none.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
			cell.add(none);
		}

		c.gridx = 1;
		c.weightx = 1;
		rows.add(cell, c);
	}

	private static AmmoType classifyWeapon(String weaponName)
	{
		if (weaponName == null)
		{
			return AmmoType.UNKNOWN;
		}
		final String n = weaponName.toLowerCase(Locale.ENGLISH);
		if (n.contains("crossbow"))
		{
			return AmmoType.BOLTS;
		}
		if (n.contains("blowpipe"))
		{
			return AmmoType.DARTS;
		}
		if (n.contains("ballista"))
		{
			return AmmoType.JAVELINS;
		}
		if (n.contains("faerdhinen") || n.contains("crystal bow"))
		{
			return AmmoType.NONE;
		}
		if (n.contains("bow"))
		{
			return AmmoType.ARROWS;
		}
		// Thrown weapons (chinchompas, knives, darts as a weapon) use no ammo slot.
		return AmmoType.NONE;
	}

	private static AmmoType classifyAmmo(String ammoName)
	{
		final String n = ammoName.toLowerCase(Locale.ENGLISH);
		if (n.contains("bolt"))
		{
			return AmmoType.BOLTS;
		}
		if (n.contains("arrow"))
		{
			return AmmoType.ARROWS;
		}
		if (n.contains("dart"))
		{
			return AmmoType.DARTS;
		}
		if (n.contains("javelin"))
		{
			return AmmoType.JAVELINS;
		}
		return AmmoType.UNKNOWN;
	}

	private static OwnedItemIndex.OwnedItem bestOwnedAmmo(OwnedItemIndex owned, AmmoType type, AttackStyle style)
	{
		OwnedItemIndex.OwnedItem best = null;
		int bestScore = Integer.MIN_VALUE;
		for (OwnedItemIndex.OwnedItem oi : owned.itemsInSlot(GearSlot.AMMO))
		{
			if (classifyAmmo(oi.name) != type)
			{
				continue;
			}
			final int s = oi.score(style);
			if (s > bestScore)
			{
				bestScore = s;
				best = oi;
			}
		}
		return best;
	}

	/** The player's owned blowpipe (Toxic / Blazing), or null if they own none. */
	private static OwnedItemIndex.OwnedItem findBlowpipe(OwnedItemIndex owned)
	{
		if (owned == null)
		{
			return null;
		}
		for (OwnedItemIndex.OwnedItem w : owned.itemsInSlot(GearSlot.WEAPON))
		{
			if (w.name.toLowerCase(Locale.ENGLISH).contains("blowpipe"))
			{
				return w;
			}
		}
		return null;
	}

	/**
	 * The four worn Void Knight pieces for {@code style} if the player owns a complete
	 * set: the style-matching helm, gloves, and a top + robe. Elite top/robe are
	 * preferred over the regular versions when owned. Returns null if any piece is
	 * missing. (The Void Knight mace and seal are intentionally excluded.)
	 */
	private static Map<GearSlot, OwnedItemIndex.OwnedItem> voidSetFor(AttackStyle style, OwnedItemIndex owned)
	{
		if (owned == null)
		{
			return null;
		}
		final String helmName = style == AttackStyle.MELEE ? "Void melee helm"
			: style == AttackStyle.RANGED ? "Void ranger helm" : "Void mage helm";
		final OwnedItemIndex.OwnedItem helm = owned.match(helmName);
		final OwnedItemIndex.OwnedItem gloves = owned.match("Void knight gloves");
		OwnedItemIndex.OwnedItem top = owned.match("Elite void top");
		if (top == null)
		{
			top = owned.match("Void knight top");
		}
		OwnedItemIndex.OwnedItem robe = owned.match("Elite void robe");
		if (robe == null)
		{
			robe = owned.match("Void knight robe");
		}
		if (helm == null || gloves == null || top == null || robe == null)
		{
			return null;
		}
		final Map<GearSlot, OwnedItemIndex.OwnedItem> set = new EnumMap<>(GearSlot.class);
		set.put(GearSlot.HEAD, helm);
		set.put(GearSlot.BODY, top);
		set.put(GearSlot.LEGS, robe);
		set.put(GearSlot.HANDS, gloves);
		return set;
	}

	/** Defence-reducing spec weapons, best first; suggested on boss tasks. */
	private static final String[] SPEC_WEAPONS = {"Elder maul", "Dragon warhammer", "Bandos godsword"};

	/**
	 * Detect a complete armour set the player owns that beats the generic picks
	 * for this style. Returns the slot-&gt;item overrides to apply (empty if none).
	 */
	private static Map<GearSlot, OwnedItemIndex.OwnedItem> computeSetBonus(
		AttackStyle style, MonsterLoadout loadout, OwnedItemIndex owned)
	{
		final Map<GearSlot, OwnedItemIndex.OwnedItem> map = new EnumMap<>(GearSlot.class);
		if (owned == null)
		{
			return map;
		}

		if (style == AttackStyle.RANGED)
		{
			// Crystal armour + a crystal bow: the set boosts crystal weapon damage & accuracy.
			final OwnedItemIndex.OwnedItem helm = owned.match("Crystal helm");
			final OwnedItemIndex.OwnedItem body = owned.match("Crystal body");
			final OwnedItemIndex.OwnedItem legs = owned.match("Crystal legs");
			OwnedItemIndex.OwnedItem bow = owned.match("Bow of faerdhinen (c)");
			if (bow == null)
			{
				bow = owned.match("Crystal bow");
			}
			if (helm != null && body != null && legs != null && bow != null)
			{
				map.put(GearSlot.HEAD, helm);
				map.put(GearSlot.BODY, body);
				map.put(GearSlot.LEGS, legs);
				map.put(GearSlot.WEAPON, bow);
			}
		}
		else if (style == AttackStyle.MELEE)
		{
			// Inquisitor's: a crush set, only worth recommending on crush-weak monsters.
			if ("CRUSH".equals(loadout.getMeleeWeakness()))
			{
				final OwnedItemIndex.OwnedItem h = owned.match("Inquisitor's great helm");
				final OwnedItemIndex.OwnedItem b = owned.match("Inquisitor's hauberk");
				final OwnedItemIndex.OwnedItem l = owned.match("Inquisitor's plateskirt");
				if (h != null && b != null && l != null)
				{
					map.put(GearSlot.HEAD, h);
					map.put(GearSlot.BODY, b);
					map.put(GearSlot.LEGS, l);
				}
			}
			// Obsidian set + an obsidian melee weapon: 10% accuracy & damage boost.
			if (map.isEmpty())
			{
				final OwnedItemIndex.OwnedItem h = owned.match("Obsidian helmet");
				final OwnedItemIndex.OwnedItem b = owned.match("Obsidian platebody");
				final OwnedItemIndex.OwnedItem l = owned.match("Obsidian platelegs");
				OwnedItemIndex.OwnedItem w = owned.match("Toktz-xil-ak");
				if (w == null)
				{
					w = owned.match("Tzhaar-ket-om");
				}
				if (w == null)
				{
					w = owned.match("Toktz-xil-ek");
				}
				if (w == null)
				{
					w = owned.match("Tzhaar-ket-em");
				}
				if (h != null && b != null && l != null && w != null)
				{
					map.put(GearSlot.HEAD, h);
					map.put(GearSlot.BODY, b);
					map.put(GearSlot.LEGS, l);
					map.put(GearSlot.WEAPON, w);
				}
			}
		}
		return map;
	}

	/** Best spellbook spell for the Magic loadout, given the chosen magic weapon. */
	private static String recommendedSpell(OwnedItemIndex.OwnedItem weapon, MonsterLoadout loadout)
	{
		final String dataSpell = loadout.getMageSpell();
		if (dataSpell != null)
		{
			return dataSpell;
		}
		if (weapon != null && isPoweredStaff(weapon.name))
		{
			return "Powered staff - no spell needed";
		}
		return "Fire Surge (Standard)";
	}

	/** Powered staffs auto-cast their own attack, so they don't use a spellbook spell. */
	private static boolean isPoweredStaff(String name)
	{
		final String n = name.toLowerCase(Locale.ENGLISH);
		return n.contains("trident") || n.contains("sanguinesti") || n.contains("shadow") || n.contains("sceptre");
	}

	private static OwnedItemIndex.OwnedItem bestSpecWeapon(OwnedItemIndex owned)
	{
		for (String n : SPEC_WEAPONS)
		{
			final OwnedItemIndex.OwnedItem m = owned.match(n);
			if (m != null)
			{
				return m;
			}
		}
		return null;
	}

	private JPanel buildSpecSection(OwnedItemIndex.OwnedItem spec, ItemManager itemManager)
	{
		final JPanel section = new JPanel(new BorderLayout());
		section.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		section.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
			BorderFactory.createEmptyBorder(6, 6, 6, 6)));

		final JLabel head = new JLabel("Special attack");
		head.setFont(FontManager.getRunescapeBoldFont());
		head.setForeground(Color.WHITE);
		head.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
		section.add(head, BorderLayout.NORTH);

		final JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(36, 32));
		final AsyncBufferedImage image = itemManager.getImage(spec.id);
		if (image != null)
		{
			image.addTo(icon);
		}
		row.add(icon);

		final JLabel name = new JLabel("<html>" + escape(spec.name)
			+ " <span style='color:#999999'>- lowers Defence</span></html>");
		name.setFont(FontManager.getRunescapeSmallFont());
		name.setForeground(Color.WHITE);
		row.add(name);

		section.add(row, BorderLayout.CENTER);
		section.setAlignmentX(Component.LEFT_ALIGNMENT);
		return section;
	}

	/** The generic top-tier melee weapons from the base data set (not bane/required). */
	private static boolean isGenericMelee(String name)
	{
		if (name == null)
		{
			return false;
		}
		switch (name.toLowerCase(Locale.ENGLISH))
		{
			case "scythe of vitur":
			case "abyssal tentacle":
			case "abyssal whip":
			case "dragon scimitar":
				return true;
			default:
				return false;
		}
	}

	private static String capitalize(String s)
	{
		if (s == null || s.isEmpty())
		{
			return s;
		}
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	private static String escape(String s)
	{
		if (s == null)
		{
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	// ---------------- Autocomplete ----------------

	/** Provide the list of known monster names to suggest (called by the plugin). */
	void setSuggestions(List<String> names)
	{
		final List<String> titled = new ArrayList<>(names.size());
		for (String n : names)
		{
			titled.add(titleCase(n));
		}
		Collections.sort(titled);
		this.suggestions = titled;
	}

	private void setupAutocomplete()
	{
		suggestionList.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		suggestionList.setForeground(Color.WHITE);
		suggestionList.setFont(FontManager.getRunescapeSmallFont());
		suggestionList.setSelectionBackground(ColorScheme.BRAND_ORANGE);
		suggestionList.setSelectionForeground(Color.WHITE);
		suggestionList.setFocusable(false);

		final JScrollPane sp = new JScrollPane(suggestionList,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		sp.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));

		suggestionWindow = new JWindow();
		suggestionWindow.setFocusableWindowState(false);
		suggestionWindow.add(sp);

		overrideField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				refreshSuggestions();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				refreshSuggestions();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				refreshSuggestions();
			}
		});

		suggestionList.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				final String value = suggestionList.getSelectedValue();
				if (value != null)
				{
					acceptSuggestion(value);
				}
			}
		});

		overrideField.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (!isSuggestVisible())
				{
					return;
				}
				final int size = suggestionModel.getSize();
				int idx = suggestionList.getSelectedIndex();
				if (e.getKeyCode() == KeyEvent.VK_DOWN)
				{
					idx = Math.min(size - 1, idx + 1);
					suggestionList.setSelectedIndex(idx);
					suggestionList.ensureIndexIsVisible(idx);
					e.consume();
				}
				else if (e.getKeyCode() == KeyEvent.VK_UP)
				{
					idx = Math.max(0, idx - 1);
					suggestionList.setSelectedIndex(idx);
					suggestionList.ensureIndexIsVisible(idx);
					e.consume();
				}
				else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					hideSuggestions();
					e.consume();
				}
			}
		});

		overrideField.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				hideSuggestions();
			}
		});
	}

	/** Apply the highlighted suggestion if any, otherwise whatever was typed. */
	private void applyFromField()
	{
		String chosen = null;
		if (isSuggestVisible() && suggestionList.getSelectedValue() != null)
		{
			chosen = suggestionList.getSelectedValue();
		}
		if (chosen == null)
		{
			final String typed = overrideField.getText().trim();
			if (!typed.isEmpty())
			{
				chosen = typed;
			}
		}
		hideSuggestions();
		if (chosen != null && !chosen.isEmpty())
		{
			listener.onManualTask(chosen);
		}
	}

	private void acceptSuggestion(String name)
	{
		suppressSuggest = true;
		overrideField.setText(name);
		suppressSuggest = false;
		hideSuggestions();
		listener.onManualTask(name);
	}

	private void refreshSuggestions()
	{
		if (suppressSuggest || suggestionWindow == null)
		{
			return;
		}
		final String typed = overrideField.getText().trim().toLowerCase(Locale.ENGLISH);
		if (typed.isEmpty())
		{
			hideSuggestions();
			return;
		}

		suggestionModel.clear();
		int count = 0;
		// Prefix matches first...
		for (String n : suggestions)
		{
			if (n.toLowerCase(Locale.ENGLISH).startsWith(typed))
			{
				suggestionModel.addElement(n);
				if (++count >= 10)
				{
					break;
				}
			}
		}
		// ...then substring matches.
		if (count < 10)
		{
			for (String n : suggestions)
			{
				final String nl = n.toLowerCase(Locale.ENGLISH);
				if (!nl.startsWith(typed) && nl.contains(typed))
				{
					suggestionModel.addElement(n);
					if (++count >= 10)
					{
						break;
					}
				}
			}
		}

		if (suggestionModel.isEmpty())
		{
			hideSuggestions();
			return;
		}
		showSuggestions();
	}

	private void showSuggestions()
	{
		if (!overrideField.isShowing())
		{
			return;
		}
		suggestionList.setVisibleRowCount(Math.min(8, suggestionModel.getSize()));
		suggestionList.clearSelection();
		suggestionWindow.pack();
		final Point p = overrideField.getLocationOnScreen();
		suggestionWindow.setSize(overrideField.getWidth(), suggestionWindow.getPreferredSize().height);
		suggestionWindow.setLocation(p.x, p.y + overrideField.getHeight());
		suggestionWindow.setVisible(true);
	}

	private void hideSuggestions()
	{
		if (suggestionWindow != null)
		{
			suggestionWindow.setVisible(false);
		}
		suggestionList.clearSelection();
	}

	private boolean isSuggestVisible()
	{
		return suggestionWindow != null && suggestionWindow.isVisible();
	}

	private static String titleCase(String s)
	{
		final String[] parts = s.split(" ");
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < parts.length; i++)
		{
			if (i > 0)
			{
				sb.append(' ');
			}
			final String p = parts[i];
			if (p.isEmpty())
			{
				continue;
			}
			sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
		}
		return sb.toString();
	}
}
