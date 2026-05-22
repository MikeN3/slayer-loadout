package com.slayerloadout;

/**
 * The three combat styles the plugin builds a loadout for.
 */
public enum AttackStyle
{
	MELEE("Melee"),
	RANGED("Ranged"),
	MAGIC("Magic");

	private final String displayName;

	AttackStyle(String displayName)
	{
		this.displayName = displayName;
	}

	public String getDisplayName()
	{
		return displayName;
	}
}
