# Changelog

All notable changes to **Slayer Loadout** are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
This plugin is distributed via the RuneLite Plugin Hub, which pins a specific commit
rather than a version tag, so version numbers below are a human-readable convenience.

## [Unreleased]

### Added
- Amulet of rancour (melee amulet) and Avernic treads (melee boots) added to the
  melee gear lists.

### Changed
- Magic loadout is now consistent with the recommended spell: when a spell is
  recommended (e.g. Ice Barrage), the plugin recommends an autocast-capable staff
  (Master wand, Kodai wand, Ancient sceptre, Staff of the dead, etc.) so the spell can
  actually be cast, rather than a powered staff (Trident/Sanguinesti/Shadow) that uses
  its own attack. Powered staves are still chosen for monsters with no recommended
  spell, shown as "no spell needed".

### Fixed
- The spell row no longer shows a spellbook spell next to a powered staff that cannot
  cast it.
- The Ancient sceptre is no longer mis-classified as a powered staff (it autocasts).

## [1.2.0] - 2026-05-24

Released via [runelite/plugin-hub#12161](https://github.com/runelite/plugin-hub/pull/12161).

### Added
- Salve amulet variants are now modelled in the DPS calculator, per variant and
  style vs undead: base/(e) boost melee only (16.67% / 20%); (i) adds ranged
  (16.67%) and magic (15%); (ei) gives 20% to all three styles. The bonus correctly
  does not stack with the slayer helmet / black mask (salve takes precedence).
- "Required gear" section: tasks with a mandatory/protective requirement now show it at
  the top, with each item's owned/missing state and an explanatory note. First use:
  Lizardmen — full tier-5 Shayzien armour to block Lizardman shaman poison (and the
  Hard Kourend & Kebos Diary tip to let your Slayer helmet count as the Shayzien helm).
  The mechanism is reusable for other special-requirement monsters.

### Changed
- Recommendations now recompute only while the bank is open (full visibility of what
  you own); swapping gear during normal play no longer recalculates them.
- Vorkath's recommended gear updated from the OSRS Wiki strategies: Salve amulet (ei)
  over Anguish (undead), Dragon hunter crossbow with ruby/diamond dragon bolts (e),
  Masori/Void and dragonfire-protection shields. The unused magic setup was removed.
- Ranged weapon DPS now includes the ammo each weapon would fire, so crossbows are
  judged with their bolts instead of at zero ranged strength (previously a bare rune
  crossbow lost to a rune knife). Blowpipe DPS likewise now counts its darts.
- With "Prefer broad bolts" on, the ranged weapon pick now favours a bolt-firing
  weapon (crossbow) when you own broad bolts and a crossbow, even over a higher-DPS
  thrown weapon like the rune knife. Turn the setting off for pure best-DPS selection.

### Fixed
- Lightbearer is now only recommended alongside a weapon that has a usable special
  attack (it speeds special-attack energy regen), instead of being paired with
  weapons that have no special.
- Non-imbued Salve amulets (base / enchanted) are no longer suggested for Ranged or
  Magic loadouts, where they give no bonus.
- The Skull sceptre is no longer recommended as a Magic weapon (its name contains
  "sceptre", which was being treated as a powered staff like the Accursed sceptre).
  The powered-staff match is now limited to the Accursed sceptre, and teleport/novelty
  items like the skull sceptre are excluded from gear recommendations.
- Crossbows are no longer paired with bolts they cannot fire. The Dorgeshuun crossbow
  (limited to bronze/iron/blurite/bone bolts) was being recommended with broad bolts
  and winning over the rune crossbow on attack speed; it now only pairs with bolts it
  can actually use, so the rune crossbow is chosen as intended.

## [1.1.0] - 2026-05-23

Released via [runelite/plugin-hub#12157](https://github.com/runelite/plugin-hub/pull/12157).

### Added
- "Prefer blowpipe" setting (off by default): when enabled and a Toxic/Blazing
  blowpipe is owned, it overrides the recommended ranged weapon and auto-selects
  your best darts.
- Void and Elite Void set bonuses are now modelled in the DPS calculator
  (melee/ranged +10% accuracy & damage; Elite ranged +12.5% damage; magic +45%
  accuracy; Elite magic +5% damage). A complete owned set is evaluated as a
  candidate loadout and chosen when it beats the best mix-and-match armour.
- Lightbearer added to the ring options.

### Changed
- Boss tasks now prefer enchanted dragon bolts (Ruby/Diamond dragon bolts (e)) for
  bolt-firing weapons; regular Slayer tasks continue to use broad bolts.

### Fixed
- Corrected an item-name mismatch in the dataset (`Archers ring` → `Archer ring`)
  so the imbued ranged ring is recognised.
- Slayer-helmet colour and imbued variants (Black, Red, Hydra, etc.) now match the
  head slot instead of falling through.
- Recommendations no longer flicker or blank out when depositing while the bank is
  open; owned-item scans are debounced to one rebuild per game tick off a
  consistent snapshot.
- A Void melee helm no longer leaks into the Ranged/Magic head slots; Void helms
  are now style-locked.

## [1.0.0] - 2026-05-23

Initial RuneLite Plugin Hub release.

### Added
- Side panel that shows the best gear you own for **Melee, Ranged, and Magic** for
  your current Slayer task.
- Automatic task detection from game chat, with a manual override and an
  autocomplete dropdown for previewing any monster.
- Scans **bank, inventory, and worn equipment** (each toggleable) and only
  recommends gear you actually own, with a best-owned fallback for empty slots.
- DPS calculator using your live (boosted) combat stats and optional offensive
  prayers; the weapon is chosen by computed DPS.
- Monster-aware heuristics: weapon-type weakness, bane gear (Salve vs. undead,
  dragonbane, demonbane), set bonuses (Crystal, Inquisitor's, Obsidian), best magic
  spell per spellbook, and spec-weapon tips on bosses.
- Weapon-aware ammo selection and a "Prefer broad bolts" setting.
- Dataset covering 126 Slayer tasks including 25 bosses, 86 with OSRS Wiki-sourced
  monster defence stats.
- 100% offline: gear data is bundled in the jar; no external network requests.
