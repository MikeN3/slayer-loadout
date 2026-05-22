package com.slayerloadout;

import java.util.List;

/**
 * Top level container that mirrors the structure of {@code bis-data.json}.
 * Populated by Gson; fields are intentionally package-private.
 */
public class BisData
{
	List<MonsterLoadout> monsters;
}
