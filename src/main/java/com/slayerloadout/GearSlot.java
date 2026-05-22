package com.slayerloadout;

/**
 * Equipment slots used by the loadout. The declaration order is the order the
 * slots are rendered in the side panel.
 */
public enum GearSlot
{
	HEAD("Head"),
	CAPE("Cape"),
	NECK("Neck"),
	WEAPON("Weapon"),
	BODY("Body"),
	SHIELD("Shield"),
	LEGS("Legs"),
	HANDS("Hands"),
	FEET("Feet"),
	RING("Ring"),
	AMMO("Ammo");

	private final String displayName;

	GearSlot(String displayName)
	{
		this.displayName = displayName;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	/**
	 * Parse a slot name from the JSON data file. Returns null for unknown slots
	 * so the data file can evolve without crashing older clients.
	 */
	static GearSlot fromKey(String key)
	{
		if (key == null)
		{
			return null;
		}
		try
		{
			return GearSlot.valueOf(key.trim().toUpperCase(java.util.Locale.ENGLISH));
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	/**
	 * Map a RuneLite equipment slot index (matching EquipmentInventorySlot's
	 * slot idx) to a {@link GearSlot}. Returns null for slots we don't display
	 * (arms, hair, jaw).
	 */
	static GearSlot fromEquipmentSlot(int idx)
	{
		switch (idx)
		{
			case 0: return HEAD;
			case 1: return CAPE;
			case 2: return NECK;
			case 3: return WEAPON;
			case 4: return BODY;
			case 5: return SHIELD;
			case 7: return LEGS;
			case 9: return HANDS;
			case 10: return FEET;
			case 12: return RING;
			case 13: return AMMO;
			default: return null;
		}
	}
}
