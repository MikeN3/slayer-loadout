package com.slayerloadout;

/**
 * A snapshot of the player's (boosted) combat levels, read from the client and
 * used by {@link DpsCalculator}.
 */
public class PlayerStats
{
	public final int attack;
	public final int strength;
	public final int defence;
	public final int ranged;
	public final int magic;
	public final int hitpoints;

	public PlayerStats(int attack, int strength, int defence, int ranged, int magic, int hitpoints)
	{
		this.attack = attack;
		this.strength = strength;
		this.defence = defence;
		this.ranged = ranged;
		this.magic = magic;
		this.hitpoints = hitpoints;
	}

	/** A sensible default (maxed) used before the client has reported real levels. */
	public static PlayerStats maxed()
	{
		return new PlayerStats(99, 99, 99, 99, 99, 99);
	}
}
