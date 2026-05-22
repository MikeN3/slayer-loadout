# Slayer Loadout

A RuneLite Plugin Hub plugin that shows, in a side panel, the **best gear you actually own** — for **Melee, Ranged, and Magic** — for whatever monster your **current Slayer task** is.

It detects your task automatically when you check it (at a Slayer Master, on an enchanted gem, or with the built-in `!task` command), looks through your **bank, inventory, and worn equipment**, and recommends the strongest full set you can equip right now for each style.

## Features

- Auto-detects your current slayer task from game chat (with a manual override in settings).
- For each combat style, shows the best item you own in every slot, walking a best→worst priority list so it always recommends the strongest gear you have.
- Highlights the recommended style per monster (★) and shows mechanic notes (e.g. *Kurask need leaf-bladed weapons*, *Gargoyles need a rock hammer*, *wear a face mask for Dust devils*).
- Looks across **bank + inventory + equipment** (each source can be toggled in settings).
- 100% offline: the gear data is bundled in the jar. The plugin makes **no external network requests**.

## How it works

1. **Task detection** — `SlayerTaskTracker` parses slayer assignment / "currently assigned" chat messages. The last task is persisted so the panel is useful immediately on login.
2. **Owned items** — `SlayerLoadoutPlugin` reads the `BANK`, `INVENTORY`, and `EQUIPMENT` item containers, canonicalises noted/placeholder items, and indexes them by name (`OwnedItemIndex`).
3. **Recommendation** — for the matched monster (`BisDataService` + `bis-data.json`), each slot has an ordered list of item names (best first); the panel shows the first one you own. Because it only displays gear you own, every shown item has a real icon.

## Editing the gear data

All recommendations live in:

```
src/main/resources/com/slayerloadout/bis-data.json
```

Each monster looks like this (lists are **best first**; the plugin shows the first item you own):

```json
{
  "names": ["abyssal demons", "abyssal demon"],
  "recommendedStyle": "MELEE",
  "notes": "Optional note shown in the panel.",
  "styles": {
    "MELEE":  { "HEAD": ["Slayer helmet (i)", "Helm of neitiznot"], "WEAPON": ["Abyssal whip", "Dragon scimitar"] },
    "RANGED": { "...": ["..."] },
    "MAGIC":  { "...": ["..."] }
  }
}
```

- `names` are the lowercase task names as they appear in chat (add aliases / singular forms here).
- Slot keys: `HEAD, CAPE, NECK, WEAPON, BODY, SHIELD, LEGS, HANDS, FEET, RING, AMMO`.
- Item names must match the in-game item name. Charged/degradable variants are matched by prefix (e.g. `"Dharok's helm"` matches an owned `"Dharok's helm 100"`).
- Omit a style entirely for monsters where it isn't viable (e.g. Kraken has only `MAGIC`/`RANGED`).

The dataset currently covers 25 common high-value tasks. Add more by following the same shape.

## Building & running (development)

Requires **Java 11** and **IntelliJ IDEA** (Community is fine), as recommended by RuneLite.

1. Open this folder in IntelliJ (*File → Open*). IntelliJ will import the Gradle project and create the Gradle wrapper automatically.
   - *Alternatively*, if you have Gradle installed on the command line, run `gradle wrapper` once in this folder to generate `gradlew` + `gradle/wrapper/gradle-wrapper.jar`, then use `./gradlew`.
2. Run the `run` Gradle task (or run `SlayerLoadoutPluginTest` — it side-loads this plugin into a dev RuneLite client).
3. The plugin appears in the sidebar with an "S" icon. Open your bank once so it can see your items, then check a slayer task.

> Note: a full Gradle build downloads the RuneLite client from `repo.runelite.net`, so you need internet access the first time.

## Publishing to the RuneLite Plugin Hub

The Plugin Hub is the public marketplace inside RuneLite. The official process:

1. **Make this a public GitHub repository.** The easiest route is to generate a repo from the [example-plugin template](https://github.com/runelite/example-plugin/generate), then copy in this project's `src/`, `runelite-plugin.properties`, `README.md`, and `LICENSE` — that template already ships the Gradle wrapper jar.
2. Confirm `runelite-plugin.properties` is filled in (it is — update `author` to your GitHub username if you like).
3. Add a `LICENSE` (BSD 2-Clause is included) and, optionally, an `icon.png` (≤ 48×72 px) at the repo root for the listing.
4. Commit and push. Note the **full 40-character commit hash** of the latest commit.
5. Fork [runelite/plugin-hub](https://github.com/runelite/plugin-hub), create a branch, and add a file `plugins/slayer-loadout` containing:

   ```
   repository=https://github.com/<you>/slayer-loadout.git
   commit=<your 40-char commit hash>
   ```

6. Open a pull request to `runelite/plugin-hub`. CI will build your plugin; fix any ❌ and push a new commit (update the `commit=` hash), keeping everything in the **one** PR.
7. A maintainer reviews it to ensure it isn't malicious and doesn't break Jagex's [Third-party client guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1). Be patient; once merged it appears in the Plugin Hub.

To **update** later, just push new commits to your repo and open a PR to plugin-hub bumping the `commit=` hash.

### Compliance notes

This plugin is built to fit within RuneLite / Jagex rules: it only **reads** game state you can already see (your task, your own bank/inventory/equipment), it doesn't automate gameplay or interact with the game on your behalf, it stores no personal data, and it makes no network calls. Avoid adding new third-party dependencies — the Plugin Hub requires cryptographic verification of any dependency that isn't already a transitive dependency of the RuneLite client, which significantly slows review.

## License

BSD 2-Clause. See [LICENSE](LICENSE).
# slayer-loadout
