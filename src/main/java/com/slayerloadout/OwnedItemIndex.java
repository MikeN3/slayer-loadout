package com.slayerloadout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.client.game.ItemEquipmentStats;

/**
 * A snapshot of the items the player currently owns (bank + inventory + worn
 * equipment), indexed both by name (for curated best-in-slot matching) and by
 * equipment slot (so we can fall back to the player's best owned item for a slot
 * when they own none of the curated picks).
 */
public class OwnedItemIndex
{
	/** A single owned, equippable item with pre-computed per-style scores. */
	public static final class OwnedItem
	{
		public final int id;
		public final String name;
		public final GearSlot slot;       // may be null for non-equippable items
		public final int meleeScore;
		public final int rangedScore;
		public final int magicScore;
		public final boolean twoHanded;
		// For weapons only: the combat style this weapon is actually meant for
		// (by its dominant attack bonus). null for non-weapon items.
		public final AttackStyle weaponStyle;
		// Raw stats kept for weakness-aware melee weapon scoring.
		public final int strength;
		public final int aStab;
		public final int aSlash;
		public final int aCrush;
		// Full equipment stats for the DPS calculator (null for non-equippable items).
		public final ItemEquipmentStats equip;

		OwnedItem(int id, String name, GearSlot slot, int meleeScore, int rangedScore, int magicScore,
			boolean twoHanded, AttackStyle weaponStyle, int strength, int aStab, int aSlash, int aCrush,
			ItemEquipmentStats equip)
		{
			this.id = id;
			this.name = name;
			this.slot = slot;
			this.meleeScore = meleeScore;
			this.rangedScore = rangedScore;
			this.magicScore = magicScore;
			this.twoHanded = twoHanded;
			this.weaponStyle = weaponStyle;
			this.strength = strength;
			this.aStab = aStab;
			this.aSlash = aSlash;
			this.aCrush = aCrush;
			this.equip = equip;
		}

		int score(AttackStyle style)
		{
			if (style == AttackStyle.MELEE)
			{
				return meleeScore;
			}
			if (style == AttackStyle.RANGED)
			{
				return rangedScore;
			}
			return magicScore;
		}

		/** Strength-weighted attack score against a specific melee defence type. */
		int meleeWeaknessScore(String weakness)
		{
			final int atk = "STAB".equals(weakness) ? aStab
				: "CRUSH".equals(weakness) ? aCrush
				: aSlash;
			return strength * 10 + atk;
		}
	}

	private final Map<String, OwnedItem> byName = new HashMap<>();
	private final Map<GearSlot, List<OwnedItem>> bySlot = new EnumMap<>(GearSlot.class);

	/** Record a non-equippable (or unclassified) owned item - name lookup only. */
	public void add(String name, int id)
	{
		if (name == null || name.isEmpty())
		{
			return;
		}
		byName.putIfAbsent(normalize(name), new OwnedItem(id, name, null, 0, 0, 0, false, null, 0, 0, 0, 0, null));
	}

	/** Record an owned equippable item with its slot and per-style scores. */
	public void addEquippable(String name, int id, GearSlot slot,
		int meleeScore, int rangedScore, int magicScore, boolean twoHanded, AttackStyle weaponStyle,
		int strength, int aStab, int aSlash, int aCrush, ItemEquipmentStats equip)
	{
		if (name == null || name.isEmpty())
		{
			return;
		}
		final OwnedItem item = new OwnedItem(id, name, slot, meleeScore, rangedScore, magicScore, twoHanded,
			weaponStyle, strength, aStab, aSlash, aCrush, equip);
		byName.putIfAbsent(normalize(name), item);
		if (slot != null)
		{
			bySlot.computeIfAbsent(slot, k -> new ArrayList<>()).add(item);
		}
	}

	/**
	 * Find an owned item matching a best-in-slot name (exact, then prefix so
	 * charged/degradable variants like "Dharok's helm 100" still match).
	 */
	public OwnedItem match(String bisName)
	{
		final String key = normalize(bisName);
		if (key.isEmpty())
		{
			return null;
		}

		final OwnedItem exact = byName.get(key);
		if (exact != null)
		{
			return exact;
		}

		for (Map.Entry<String, OwnedItem> entry : byName.entrySet())
		{
			final String owned = entry.getKey();
			if (owned.startsWith(key + " ") || owned.startsWith(key + "("))
			{
				return entry.getValue();
			}
		}

		// Slayer helmet colour/boss variants (Black, Red, Hydra, Twisted, Tztok, ...)
		// all END with the base name, so a suffix match catches them. The "(i)" curated
		// entry is listed first, so imbued variants are preferred over un-imbued.
		if (key.equals("slayer helmet (i)") || key.equals("slayer helmet"))
		{
			for (Map.Entry<String, OwnedItem> entry : byName.entrySet())
			{
				if (entry.getKey().endsWith(key))
				{
					return entry.getValue();
				}
			}
		}
		return null;
	}

	/**
	 * The best owned item for a slot, scored for the given attack style.
	 *
	 * <p>For the weapon slot, only weapons actually built for that style are
	 * considered (so a melee spear can't be suggested for a Magic loadout).
	 * Items with no useful stats (cosmetics) are skipped.</p>
	 */
	public OwnedItem bestInSlot(GearSlot slot, AttackStyle style)
	{
		final List<OwnedItem> list = bySlot.get(slot);
		if (list == null || list.isEmpty())
		{
			return null;
		}

		OwnedItem best = null;
		int bestScore = 0; // strictly-positive required, which excludes 0-stat cosmetics
		for (OwnedItem item : list)
		{
			if (slot == GearSlot.WEAPON && item.weaponStyle != style)
			{
				continue;
			}
			// A Void helm only helps its own style (it unlocks that style's Void set
			// bonus and gives nothing useful otherwise), so never let the melee helm
			// fill a Ranged/Magic head slot, and vice-versa.
			if (slot == GearSlot.HEAD && isMismatchedVoidHelm(item.name, style))
			{
				continue;
			}
			final int s = item.score(style);
			if (s > bestScore)
			{
				bestScore = s;
				best = item;
			}
		}
		return best;
	}

	/**
	 * True if {@code name} is a Void Knight helm built for a combat style other than
	 * {@code style}. The Void melee/ranger/mage helms are style-locked, so each must
	 * only ever appear in its own style's loadout.
	 */
	private static boolean isMismatchedVoidHelm(String name, AttackStyle style)
	{
		final String n = name.toLowerCase(Locale.ENGLISH);
		if (!n.contains("void"))
		{
			return false;
		}
		if (n.contains("melee"))
		{
			return style != AttackStyle.MELEE;
		}
		if (n.contains("ranger"))
		{
			return style != AttackStyle.RANGED;
		}
		if (n.contains("mage"))
		{
			return style != AttackStyle.MAGIC;
		}
		return false;
	}

	/** All owned items in a slot (used for weapon-aware ammo selection). */
	public List<OwnedItem> itemsInSlot(GearSlot slot)
	{
		return bySlot.getOrDefault(slot, Collections.emptyList());
	}

	/** All owned weapons whose intended combat style matches the given style. */
	public List<OwnedItem> weaponsForStyle(AttackStyle style)
	{
		final List<OwnedItem> all = bySlot.getOrDefault(GearSlot.WEAPON, Collections.emptyList());
		final List<OwnedItem> out = new ArrayList<>();
		for (OwnedItem w : all)
		{
			if (w.weaponStyle == style)
			{
				out.add(w);
			}
		}
		return out;
	}

	/**
	 * The best owned melee weapon against a given defence weakness
	 * ("STAB"/"SLASH"/"CRUSH"), scored by strength + that attack type.
	 */
	public OwnedItem bestMeleeWeaponForWeakness(String weakness)
	{
		final List<OwnedItem> list = bySlot.get(GearSlot.WEAPON);
		if (list == null)
		{
			return null;
		}
		OwnedItem best = null;
		int bestScore = 0;
		for (OwnedItem item : list)
		{
			if (item.weaponStyle != AttackStyle.MELEE)
			{
				continue;
			}
			final int s = item.meleeWeaknessScore(weakness);
			if (s > bestScore)
			{
				bestScore = s;
				best = item;
			}
		}
		return best;
	}

	public int size()
	{
		return byName.size();
	}

	static String normalize(String s)
	{
		return s == null ? "" : s.toLowerCase(Locale.ENGLISH).trim();
	}
}
