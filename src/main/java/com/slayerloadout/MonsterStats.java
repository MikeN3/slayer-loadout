package com.slayerloadout;

/**
 * Combat/defence stats for a monster, used by the DPS calculator. Populated by
 * Gson from the optional {@code combat} block in {@code bis-data.json}; fields
 * are package-private and may be absent (then the plugin falls back to heuristics).
 */
public class MonsterStats
{
	int hitpoints;
	int defenceLevel;
	int magicLevel;

	// Defensive bonuses by attack type.
	int defStab;
	int defSlash;
	int defCrush;
	int defMagic;
	int defRange;

	/** NPC size in tiles (1 = small). Used for multi-hit weapons like the Scythe. */
	int size = 1;

	int defenceForStyle(AttackStyle style, int meleeType)
	{
		if (style == AttackStyle.RANGED)
		{
			return defRange;
		}
		if (style == AttackStyle.MAGIC)
		{
			return defMagic;
		}
		// meleeType: 0 = stab, 1 = slash, 2 = crush
		if (meleeType == 0)
		{
			return defStab;
		}
		if (meleeType == 2)
		{
			return defCrush;
		}
		return defSlash;
	}
}
