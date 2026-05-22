# Slayer Loadout

A RuneLite Plugin Hub plugin that shows, in a side panel, the **best gear you actually own** — for **Melee, Ranged, and Magic** — for whatever monster your **current Slayer task** is.

It detects your task automatically when you check it (at a Slayer Master or on an enchanted gem), looks through your **bank, inventory, and worn equipment**, and recommends the strongest full set you can equip right now for each style. Everything it shows is gear you own, so every item has a real icon and is actually wearable.

## Features

- **Auto-detects your current slayer task** from game chat, and remembers it across logins. You can also type any monster into the panel (with an autocomplete dropdown) to preview a loadout, then click **Back to my task**.
- **Best-in-slot from your own gear**, per style. Each slot walks a best→worst priority list and shows the strongest item you own; empty slots fall back to your best available piece rather than showing nothing.
- **Built-in DPS estimate.** A real OSRS combat calculator (effective levels, max hit, accuracy) ranks your weapons using your **live, boosted** combat stats and picks the highest-DPS option per style. Estimated DPS is shown in each style's header. Offensive prayers (Piety / Rigour / Augury) can be included via a setting.
- **Monster-aware heuristics:**
  - **Weapon weakness** — prefers stab / slash / crush to match the monster's weakness.
  - **Bane gear** via monster attributes — e.g. Salve amulet vs. undead, dragonbane weapons vs. dragons, demonbane (Arclight / Emberlight) vs. demons.
  - **Set bonuses** — recognises Crystal armour + Bow of faerdhinen, Inquisitor's (crush), and Obsidian sets.
  - **Best magic spell** for your spellbook (Standard / Ancient / Arceuus / Lunar).
  - **Spec-weapon tips** on bosses (e.g. when a Dragon warhammer / Bandos godsword special is worth bringing).
  - **Mechanic notes** (e.g. *Kurask need leaf-bladed weapons*, *Gargoyles need a rock hammer*, *wear a face mask for Dust devils*), plus a highlight of the recommended style.
- **Weapon-aware ammo** — crossbows get bolts, bows get arrows, never a mismatched pair. An optional **Prefer broad bolts** setting (on by default) uses broad bolts whenever a bolt-firing weapon is recommended, since they damage almost every Slayer monster.
- **Covers every Slayer task** — all 126 assignable monsters including the 25 Slayer bosses. 86 of them carry full monster defence stats sourced from the OSRS Wiki for accurate DPS; the rest fall back to sensible heuristics.
- **100% offline.** The gear data is bundled in the jar. The plugin only **reads** game state you can already see and makes **no external network requests**.

## Settings

- **Manual task override** — type a monster name to override auto-detection. Leave blank to auto-detect from chat.
- **Include bank / inventory / worn equipment** — toggle which sources are searched for owned gear (the bank must be opened once per session to be visible).
- **Prefer broad bolts** — use broad bolts for the ammo slot when a bolt-firing weapon is recommended (default on).
- **Assume offensive prayers** — include the standard offensive prayer (Piety / Rigour / Augury) in the DPS estimate (default on).

## How it works

1. **Task detection** — `SlayerTaskTracker` parses slayer assignment / "currently assigned" chat messages. The last task is persisted so the panel is useful immediately on login.
2. **Owned items** — `SlayerLoadoutPlugin` reads the `BANK`, `INVENTORY`, and `EQUIPMENT` item containers, canonicalises noted/placeholder items, captures each equippable item's combat stats, and indexes everything by name (`OwnedItemIndex`).
3. **Recommendation** — for the matched monster (`BisDataService` + `bis-data.json`), each slot has an ordered list of item names (best first). Non-weapon slots show the first item you own; weapons are chosen by the `DpsCalculator` using the monster's stats and your live combat levels. Because it only displays gear you own, every shown item has a real icon.

## Editing the gear data

All recommendations live in:

```
src/main/resources/com/slayerloadout/bis-data.json
```

The file is a single object with a `monsters` array. Each monster looks like this (item lists are **best first**; the plugin shows the first one you own):

```json
{
  "names": ["bloodvelds", "bloodveld", "mutated bloodveld"],
  "slayerLevel": 50,
  "difficulty": "Medium",
  "recommendedStyle": "MELEE",
  "attributes": ["demon"],
  "meleeWeakness": "SLASH",
  "mageSpell": "Ice Barrage (Ancient)",
  "combat": {
    "defenceLevel": 30,
    "defStab": 0, "defSlash": 0, "defCrush": 0,
    "defMagic": 0, "defRange": 0,
    "magicLevel": 1, "hitpoints": 120, "size": 2
  },
  "notes": "Optional note shown in the panel.",
  "styles": {
    "MELEE":  { "HEAD": ["Slayer helmet (i)", "Helm of neitiznot"], "WEAPON": ["Abyssal whip", "Dragon scimitar"] },
    "RANGED": { "...": ["..."] },
    "MAGIC":  { "...": ["..."] }
  }
}
```

- `names` are the lowercase task names as they appear in chat (add aliases / singular forms here).
- `recommendedStyle` / `meleeWeakness` are `MELEE`, `RANGED`, `MAGIC` and `STAB`, `SLASH`, `CRUSH` respectively.
- `attributes` drive bane-gear logic — e.g. `"undead"`, `"dragon"`, `"demon"`.
- `combat` is optional. When present, the DPS calculator uses it; when omitted, the plugin falls back to heuristics.
- Slot keys: `HEAD, CAPE, NECK, WEAPON, BODY, SHIELD, LEGS, HANDS, FEET, RING, AMMO`.
- Item names must match the in-game item name. Charged/degradable variants are matched by prefix (e.g. `"Dharok's helm"` matches an owned `"Dharok's helm 100"`).
- Omit a style entirely for monsters where it isn't viable (e.g. Kraken has only `MAGIC` / `RANGED`).

## Building & running (development)

Requires **Java 11** and **IntelliJ IDEA** (Community is fine), as recommended by RuneLite.

1. Open this folder in IntelliJ (*File → Open*). IntelliJ imports the Gradle project and creates the Gradle wrapper automatically.
   - *Alternatively*, with Gradle on the command line, run `gradle wrapper` once to generate `gradlew` + `gradle/wrapper/gradle-wrapper.jar`, then use `./gradlew`.
2. Run the `run` Gradle task (or run `SlayerLoadoutPluginTest` — it side-loads this plugin into a dev RuneLite client).
3. The plugin appears in the sidebar with an "S" icon. Open your bank once so it can see your items, then check a slayer task.

> Note: a full Gradle build downloads the RuneLite client from `repo.runelite.net`, so you need internet access the first time.

## Compliance notes

This plugin fits within RuneLite / Jagex rules: it only **reads** game state you can already see (your task, your own bank/inventory/equipment), it doesn't automate gameplay or interact with the game on your behalf, it stores no personal data, and it makes no network calls. It adds no third-party dependencies beyond what the RuneLite client already provides, and reuses the client's injected `Gson`.

## License

BSD 2-Clause. See [LICENSE](LICENSE).
