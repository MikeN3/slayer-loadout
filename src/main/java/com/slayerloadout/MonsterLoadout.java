package com.slayerloadout;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Best-in-slot recommendations for a single slayer monster.
 *
 * <p>The {@code styles} map is keyed by {@link AttackStyle} name (MELEE / RANGED /
 * MAGIC). Each style maps {@link GearSlot} name to an <b>ordered</b> list of item
 * names, best first. The plugin walks the list and picks the first item the player
 * actually owns, so the lists double as a graceful "downgrade" path.</p>
 *
 * <p>Populated by Gson; fields are package-private by design.</p>
 */
public class MonsterLoadout
{
	/** Names / aliases that appear in the slayer assignment chat message. */
	List<String> names;

	/** "MELEE", "RANGED" or "MAGIC" - the style this plugin highlights first. */
	String recommendedStyle;

	/** Free-form note shown in the panel (mechanics, finishing items, etc.). */
	String notes;

	/** Melee defence weakness ("STAB"/"SLASH"/"CRUSH") used to pick a melee weapon. */
	String meleeWeakness;

	/** Difficulty tier ("Easy"/"Medium"/"Hard"/"Boss"); used for spec-weapon tips. */
	String difficulty;

	/** Recommended spellbook spell for the Magic loadout (e.g. "Ice Barrage (Ancient)"). */
	String mageSpell;

	/** Optional combat/defence stats for the DPS calculator (null = use heuristics). */
	MonsterStats combat;

	/** Combat attributes (e.g. "dragon", "undead", "demon") for bane modifiers. */
	List<String> attributes;

	/**
	 * Protective / mandatory gear for a special mechanic (e.g. Shayzien armour to block
	 * lizardman shaman poison). Surfaced separately from the DPS loadout because it's a
	 * survivability requirement, not a damage choice.
	 */
	List<String> requiredGear;

	/** Explanation shown alongside {@link #requiredGear}. */
	String requiredGearNote;

	/**
	 * True when the task mandates a shield (e.g. an anti-wyvern shield to block icy
	 * breath). A shield occupies the off-hand, so only one-handed weapons may be
	 * recommended - a two-handed weapon would leave no room for the required shield.
	 */
	boolean requiresShield;

	/** styleName -> (slotName -> ordered list of item names, best first). */
	Map<String, Map<String, List<String>>> styles;

	List<String> getNames()
	{
		return names == null ? Collections.emptyList() : names;
	}

	String getNotes()
	{
		return notes;
	}

	boolean isBoss()
	{
		return "Boss".equalsIgnoreCase(difficulty);
	}

	/** @return the recommended spellbook spell, or null to let the panel decide. */
	String getMageSpell()
	{
		return (mageSpell == null || mageSpell.trim().isEmpty()) ? null : mageSpell.trim();
	}

	/** @return combat/defence stats for the DPS calc, or null if not seeded. */
	MonsterStats getCombat()
	{
		return combat;
	}

	List<String> getAttributes()
	{
		return attributes == null ? Collections.emptyList() : attributes;
	}

	/** @return mandatory/protective gear item names for this task, or an empty list. */
	List<String> getRequiredGear()
	{
		return requiredGear == null ? Collections.emptyList() : requiredGear;
	}

	/** @return the note shown with the required gear, or null. */
	String getRequiredGearNote()
	{
		return (requiredGearNote == null || requiredGearNote.trim().isEmpty()) ? null : requiredGearNote.trim();
	}

	/** @return true if a shield is mandatory, so only one-handed weapons may be recommended. */
	boolean isRequiresShield()
	{
		return requiresShield;
	}

	/** @return "STAB"/"SLASH"/"CRUSH", or null if no weakness is set/recognised. */
	String getMeleeWeakness()
	{
		if (meleeWeakness == null)
		{
			return null;
		}
		final String w = meleeWeakness.trim().toUpperCase(java.util.Locale.ENGLISH);
		return ("STAB".equals(w) || "SLASH".equals(w) || "CRUSH".equals(w)) ? w : null;
	}

	AttackStyle getRecommendedStyle()
	{
		if (recommendedStyle == null)
		{
			return null;
		}
		try
		{
			return AttackStyle.valueOf(recommendedStyle.trim().toUpperCase(java.util.Locale.ENGLISH));
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	/**
	 * @return the ordered item-name lists per slot for the given style, or an
	 * empty map if the style is not viable / not defined for this monster.
	 */
	Map<String, List<String>> getStyle(AttackStyle style)
	{
		if (styles == null || style == null)
		{
			return Collections.emptyMap();
		}
		Map<String, List<String>> m = styles.get(style.name());
		return m == null ? Collections.emptyMap() : m;
	}

	boolean hasStyle(AttackStyle style)
	{
		return !getStyle(style).isEmpty();
	}
}
