package com.slayerloadout;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.client.game.ItemEquipmentStats;

/**
 * A first-cut OSRS DPS calculator. Implements the standard max-hit / accuracy /
 * DPS formulas for melee, ranged and (approximate) magic, plus the major
 * on-task and weapon modifiers (Slayer helmet, Salve, Dragon hunter, demonbane,
 * Twisted bow, Tumeken's shadow, Scythe, Void).
 *
 * <p>Assumptions for v1: aggressive melee / rapid ranged stances, the standard
 * offensive prayer per style when {@code prayers} is true, and magic damage from
 * a powered staff (level-based) or a generic Fire Surge base for cast spells.
 * Values are estimates intended for comparing loadouts, not exact simulation.</p>
 */
public final class DpsCalculator
{
	private DpsCalculator()
	{
	}

	/** Compute estimated DPS for a fully-specified loadout against a monster. */
	public static double computeDps(AttackStyle style, PlayerStats p, boolean prayers,
		Map<GearSlot, OwnedItemIndex.OwnedItem> loadout, MonsterStats m, List<String> attributes)
	{
		if (p == null || m == null)
		{
			return 0;
		}
		final OwnedItemIndex.OwnedItem weapon = loadout.get(GearSlot.WEAPON);
		if (weapon == null || weapon.equip == null)
		{
			return 0;
		}

		int aStab = 0, aSlash = 0, aCrush = 0, aMagic = 0, aRange = 0;
		int meleeStr = 0, rangedStr = 0, magicDmg = 0;
		for (OwnedItemIndex.OwnedItem it : loadout.values())
		{
			if (it == null || it.equip == null)
			{
				continue;
			}
			final ItemEquipmentStats e = it.equip;
			aStab += e.getAstab();
			aSlash += e.getAslash();
			aCrush += e.getAcrush();
			aMagic += e.getAmagic();
			aRange += e.getArange();
			meleeStr += e.getStr();
			rangedStr += e.getRstr();
			magicDmg += (int) e.getMdmg();
		}

		final String wname = weapon.name.toLowerCase(Locale.ENGLISH);
		final boolean undead = attributes != null && attributes.contains("undead");
		final boolean dragon = attributes != null && attributes.contains("dragon");
		final boolean demon = attributes != null && attributes.contains("demon");
		final VoidKind vk = voidKind(loadout, style);
		final boolean voidSet = vk != VoidKind.NONE;

		final double[] gm = generalMultiplier(loadout, undead);
		double accMult = gm[0];
		double dmgMult = gm[1];

		int speed = weapon.equip.getAspeed();
		if (speed <= 0)
		{
			speed = 4;
		}

		if (style == AttackStyle.MELEE)
		{
			if (dragon && wname.contains("dragon hunter lance"))
			{
				accMult *= 1.20;
				dmgMult *= 1.20;
			}
			if (demon && (wname.contains("arclight") || wname.contains("emberlight") || wname.contains("darklight")))
			{
				accMult *= 1.70;
				dmgMult *= 1.70;
			}
			double best = 0;
			for (int type = 0; type < 3; type++)
			{
				final int atkBonus = type == 0 ? aStab : type == 2 ? aCrush : aSlash;
				final int defBonus = m.defenceForStyle(AttackStyle.MELEE, type);
				final double d = meleeDps(p, prayers, atkBonus, meleeStr, speed, m.defenceLevel, defBonus, accMult, dmgMult, voidSet);
				if (d > best)
				{
					best = d;
				}
			}
			if (wname.contains("scythe of vitur") && m.size >= 2)
			{
				best *= 1.75; // approximate value of the second and third hits
			}
			return best;
		}

		if (style == AttackStyle.RANGED)
		{
			if (dragon && wname.contains("dragon hunter crossbow"))
			{
				accMult *= 1.30;
				dmgMult *= 1.25;
			}
			if (wname.contains("twisted bow"))
			{
				final int mag = Math.max(0, Math.min(m.magicLevel, 350));
				final double accBoost = (140.0 + (3.0 * mag - 10) / 100.0
					- Math.pow((3.0 * mag / 10.0 - 100), 2) / 100.0) / 100.0;
				final double dmgBoost = (250.0 + (3.0 * mag - 14) / 100.0
					- Math.pow((3.0 * mag / 10.0 - 140), 2) / 100.0) / 100.0;
				accMult *= clamp(accBoost, 0.0, 1.40);
				dmgMult *= clamp(dmgBoost, 0.0, 2.50);
			}
			if (vk == VoidKind.ELITE)
			{
				dmgMult *= 1.025; // Elite Void ranged: +2.5% damage on top of the base +10% set.
			}
			final int rapidSpeed = Math.max(1, speed - 1);
			return rangedDps(p, prayers, aRange, rangedStr, rapidSpeed, m.defenceLevel, m.defRange, accMult, dmgMult, voidSet);
		}

		// MAGIC
		boolean shadow = wname.contains("shadow");
		if (shadow)
		{
			magicDmg *= 3;
			aMagic *= 3;
		}
		if (vk != VoidKind.NONE)
		{
			accMult *= 1.45; // Void mage set: +45% magic accuracy.
			if (vk == VoidKind.ELITE)
			{
				magicDmg += 5; // Elite Void: +5% magic damage.
			}
		}
		final int baseMax = magicBaseMax(wname, p.magic);
		final int magicSpeed = isPoweredStaffName(wname) ? speed : 5;
		return magicDps(p, prayers, aMagic, magicDmg, baseMax, magicSpeed, m.magicLevel, m.defMagic, accMult, dmgMult);
	}

	private static double meleeDps(PlayerStats p, boolean prayers, int atkBonus, int strBonus, int speed,
		int monDefLevel, int monDefBonus, double accMult, double dmgMult, boolean voidSet)
	{
		final double strPray = prayers ? 1.23 : 1.0;
		final double attPray = prayers ? 1.20 : 1.0;
		int effStr = (int) Math.floor(p.strength * strPray) + 3 + 8;
		int effAtt = (int) Math.floor(p.attack * attPray) + 8;
		if (voidSet)
		{
			effStr = (int) Math.floor(effStr * 1.10);
			effAtt = (int) Math.floor(effAtt * 1.10);
		}
		int maxHit = (int) Math.floor(0.5 + effStr * (strBonus + 64) / 640.0);
		maxHit = (int) Math.floor(maxHit * dmgMult);
		final double attRoll = effAtt * (atkBonus + 64) * accMult;
		final double defRoll = (monDefLevel + 9) * (monDefBonus + 64);
		final double hc = hitChance(attRoll, defRoll);
		return (hc * maxHit / 2.0) / (speed * 0.6);
	}

	private static double rangedDps(PlayerStats p, boolean prayers, int atkBonus, int strBonus, int speed,
		int monDefLevel, int monDefBonus, double accMult, double dmgMult, boolean voidSet)
	{
		final double pray = prayers ? 1.23 : 1.0;
		final double attPray = prayers ? 1.20 : 1.0;
		int effStr = (int) Math.floor(p.ranged * pray) + 8;
		int effAtt = (int) Math.floor(p.ranged * attPray) + 8;
		if (voidSet)
		{
			effStr = (int) Math.floor(effStr * 1.10);
			effAtt = (int) Math.floor(effAtt * 1.10);
		}
		int maxHit = (int) Math.floor(0.5 + effStr * (strBonus + 64) / 640.0);
		maxHit = (int) Math.floor(maxHit * dmgMult);
		final double attRoll = effAtt * (atkBonus + 64) * accMult;
		final double defRoll = (monDefLevel + 9) * (monDefBonus + 64);
		final double hc = hitChance(attRoll, defRoll);
		return (hc * maxHit / 2.0) / (speed * 0.6);
	}

	private static double magicDps(PlayerStats p, boolean prayers, int magicAttBonus, int magicDmgBonus,
		int baseMax, int speed, int monMagicLevel, int monMagicDef, double accMult, double dmgMult)
	{
		int maxHit = (int) Math.floor(baseMax * (1.0 + magicDmgBonus / 100.0));
		maxHit = (int) Math.floor(maxHit * dmgMult);
		final double magPray = prayers ? 1.25 : 1.0;
		final int effMagic = (int) Math.floor(p.magic * magPray) + 9;
		final double attRoll = effMagic * (magicAttBonus + 64) * accMult;
		final double defRoll = (monMagicLevel + 9) * (monMagicDef + 64);
		final double hc = hitChance(attRoll, defRoll);
		return (hc * maxHit / 2.0) / (speed * 0.6);
	}

	private static double hitChance(double attRoll, double defRoll)
	{
		if (attRoll > defRoll)
		{
			return 1.0 - (defRoll + 2.0) / (2.0 * (attRoll + 1.0));
		}
		return attRoll / (2.0 * (defRoll + 1.0));
	}

	private static double[] generalMultiplier(Map<GearSlot, OwnedItemIndex.OwnedItem> loadout, boolean undead)
	{
		final OwnedItemIndex.OwnedItem neck = loadout.get(GearSlot.NECK);
		final OwnedItemIndex.OwnedItem head = loadout.get(GearSlot.HEAD);
		final boolean salve = neck != null && neck.name.toLowerCase(Locale.ENGLISH).contains("salve amulet");
		final boolean slayerHelm = head != null
			&& (head.name.toLowerCase(Locale.ENGLISH).contains("slayer helmet")
			|| head.name.toLowerCase(Locale.ENGLISH).contains("black mask"));
		double mult = 1.0;
		if (undead && salve)
		{
			mult = 1.20;
		}
		else if (slayerHelm)
		{
			mult = 1.0 + 1.0 / 6.0;
		}
		return new double[]{mult, mult};
	}

	private static int magicBaseMax(String wname, int magicLevel)
	{
		final int third = magicLevel / 3;
		if (wname.contains("shadow"))
		{
			return third + 1;
		}
		if (wname.contains("sanguinesti"))
		{
			return third - 1;
		}
		if (wname.contains("trident of the swamp"))
		{
			return third - 2;
		}
		if (wname.contains("trident of the seas"))
		{
			return third - 5;
		}
		if (wname.contains("sceptre"))
		{
			return third - 3;
		}
		// Cast spell (non-powered weapon): assume a high-tier surge.
		return 24;
	}

	private static boolean isPoweredStaffName(String wname)
	{
		return wname.contains("trident") || wname.contains("sanguinesti")
			|| wname.contains("shadow") || wname.contains("sceptre");
	}

	private enum VoidKind
	{
		NONE, REGULAR, ELITE
	}

	/**
	 * Detect a complete, style-correct Void Knight set in the loadout. The worn helm
	 * must match {@code style} (melee/ranger/mage), and the gloves plus a top and robe
	 * (regular or elite) are all required. ELITE is only returned when BOTH the top
	 * and robe are the elite versions, matching the in-game set effect.
	 */
	private static VoidKind voidKind(Map<GearSlot, OwnedItemIndex.OwnedItem> loadout, AttackStyle style)
	{
		final OwnedItemIndex.OwnedItem head = loadout.get(GearSlot.HEAD);
		final OwnedItemIndex.OwnedItem body = loadout.get(GearSlot.BODY);
		final OwnedItemIndex.OwnedItem legs = loadout.get(GearSlot.LEGS);
		final OwnedItemIndex.OwnedItem hands = loadout.get(GearSlot.HANDS);
		if (head == null || body == null || legs == null || hands == null)
		{
			return VoidKind.NONE;
		}
		final String h = head.name.toLowerCase(Locale.ENGLISH);
		final String b = body.name.toLowerCase(Locale.ENGLISH);
		final String l = legs.name.toLowerCase(Locale.ENGLISH);
		final String g = hands.name.toLowerCase(Locale.ENGLISH);
		if (!h.contains("void") || !b.contains("void") || !l.contains("void") || !g.contains("void"))
		{
			return VoidKind.NONE;
		}
		final boolean helmMatches =
			(style == AttackStyle.MELEE && h.contains("melee"))
			|| (style == AttackStyle.RANGED && h.contains("ranger"))
			|| (style == AttackStyle.MAGIC && h.contains("mage"));
		if (!helmMatches)
		{
			return VoidKind.NONE;
		}
		return (b.contains("elite") && l.contains("elite")) ? VoidKind.ELITE : VoidKind.REGULAR;
	}

	private static double clamp(double v, double lo, double hi)
	{
		return Math.max(lo, Math.min(hi, v));
	}
}
