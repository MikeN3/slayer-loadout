package com.slayerloadout;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(SlayerLoadoutConfig.GROUP)
public interface SlayerLoadoutConfig extends Config
{
	String GROUP = "slayerloadout";

	@ConfigItem(
		keyName = "manualTask",
		name = "Manual task override",
		description = "Type a monster name to override auto-detection (e.g. 'abyssal demons'). Leave blank to auto-detect from chat.",
		position = 0
	)
	default String manualTask()
	{
		return "";
	}

	@ConfigItem(
		keyName = "includeBank",
		name = "Include bank",
		description = "Include items in your bank when finding owned gear. The bank must be opened at least once per session for its contents to be visible.",
		position = 1
	)
	default boolean includeBank()
	{
		return true;
	}

	@ConfigItem(
		keyName = "includeInventory",
		name = "Include inventory",
		description = "Include items in your inventory when finding owned gear.",
		position = 2
	)
	default boolean includeInventory()
	{
		return true;
	}

	@ConfigItem(
		keyName = "includeEquipment",
		name = "Include worn equipment",
		description = "Include items you are currently wearing when finding owned gear.",
		position = 3
	)
	default boolean includeEquipment()
	{
		return true;
	}

	@ConfigItem(
		keyName = "preferBroadBolts",
		name = "Prefer broad bolts",
		description = "When the recommended ranged weapon is a crossbow (or any bolt-firing weapon), use Broad bolts for the ammo slot if you own them. Ideal for Slayer since broad bolts damage almost every monster.",
		position = 4
	)
	default boolean preferBroadBolts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "assumePrayers",
		name = "Assume offensive prayers",
		description = "Include the standard offensive prayer (Piety / Rigour / Augury) in the DPS estimate.",
		position = 5
	)
	default boolean assumePrayers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "preferBlowpipe",
		name = "Prefer blowpipe",
		description = "When enabled, if you own a Toxic/Blazing blowpipe it overrides the recommended ranged weapon and uses your best darts as ammo. Off by default.",
		position = 6
	)
	default boolean preferBlowpipe()
	{
		return false;
	}

	// Persisted internally so the last task survives a client restart. Hidden from the UI.
	@ConfigItem(
		keyName = "lastTask",
		name = "",
		description = "",
		hidden = true
	)
	default String lastTask()
	{
		return "";
	}
}
