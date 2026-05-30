# Changelog

All notable changes to **Slayer Loadout** are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
This plugin is distributed via the RuneLite Plugin Hub, which pins a specific commit
rather than a version tag, so version numbers below are a human-readable convenience.

## [Unreleased]

## [1.8.0] - 2026-05-30

Released via [runelite/plugin-hub#12290](https://github.com/runelite/plugin-hub/pull/12290).

### Added
- **Royal Titans** added to the dataset - the duo boss (Branda the Fire Queen and Eldric
  the Ice King) in the Asgarnia Ice Dungeon. Counts toward both Fire giants and Ice
  giants Slayer tasks and is searchable via the preview field. Includes per-style BiS
  pulled from the OSRS Wiki Strategies page and a Warning section flagging the Twinflame
  staff (auto-swaps Fire/Water spells for walls and elementals) and the Antler guard
  ranged off-hand. The other common counts-for bosses (Vorkath, Cerberus, Kraken,
  Alchemical Hydra, Abyssal Sire) were already in the dataset and remain searchable.

## [1.7.0] - 2026-05-26

Released via [runelite/plugin-hub#12215](https://github.com/runelite/plugin-hub/pull/12215).

### Added
- **Metal Dragons** task support. Metal dragons is a multi-monster task, so the panel now
  shows a picker with a button for each of the six dragons (Bronze, Iron, Steel, Mithril,
  Adamant, Rune). It starts blank until you choose one, then shows that dragon's
  best-in-slot loadout and DPS. Each dragon remains previewable on its own too.
- A **Warning** section: a soft, non-mandatory counterpart to Required gear that flags
  suggested items for a monster's special mechanics - e.g. Insulated boots for Rune
  dragons' Electricity special, Serpentine helm for Adamant dragons' poison spit, and a
  super antifire reminder for Mithril dragons' long-range dragonfire.

## [1.6.0] - 2026-05-25

Released via [runelite/plugin-hub#12201](https://github.com/runelite/plugin-hub/pull/12201).

### Added
- Recommendation rows are now colour-coded by where the item is: green when you have it
  worn or in your inventory (ready to use right now), blue when it's part of a
  recommended set bonus, and white for everything else you own (i.e. in the bank).

### Changed
- The old "best owned alternative" blue colour has been removed - curated picks and
  owned alternatives now both render in white, and the set-bonus highlight moved from
  green to blue so green is reserved for the new worn/inventory indicator.

## [1.5.1] - 2026-05-25

Released via [runelite/plugin-hub#12191](https://github.com/runelite/plugin-hub/pull/12191).

### Fixed
- Eye of Ayak is now treated as a powered staff (it has a built-in attack), so the
  Magic loadout no longer suggests a spellbook spell next to it - it can only use its
  own attack. Its DPS is also modelled at its true 3-tick speed and base max hit of
  floor(Magic level / 3) - 6.
- Tasks that mandate a shield (e.g. Fossil Island wyverns and their anti-wyvern
  shield) no longer recommend a two-handed weapon, which would leave no off-hand for
  the required shield. Only one-handed weapons are considered for these tasks, across
  the curated pick, the DPS pick, and the best-owned fallback.

## [1.5.0] - 2026-05-24

Released via [runelite/plugin-hub#12182](https://github.com/runelite/plugin-hub/pull/12182).

### Added
- Confliction gauntlets added as best-in-slot Magic hands (above the Tormented
  bracelet). Note: its passive gives the next cast a second accuracy roll after a miss
  on the same target - but only with one-handed magic weapons (disabled with
  two-handers such as Tumeken's shadow).
- Avernic treads variations added across the feet slots: all eight forms (base,
  (pr)/(pe)/(et), the three combos, and (max)). Pegasian forms appear for Ranged and
  eternal forms for Magic, with (max) - the highest offensive boots for all three
  styles - at the top; the melee feet entry already covers the melee forms. Note: they
  give no prayer bonus and slightly less defence than Guardian/Echo boots.

## [1.4.0] - 2026-05-24

Released via [runelite/plugin-hub#12179](https://github.com/runelite/plugin-hub/pull/12179).

### Added
- Fossil Island wyverns task added, with Melee/Ranged/Magic BiS from the OSRS Wiki and
  wiki defence stats for the DPS estimate. The mandatory anti-wyvern shield - any one of
  Ancient wyvern shield, Dragonfire ward/shield, Mind or Elemental shield (an Anti-dragon
  shield does NOT work) - is surfaced as a Required gear callout for the icy breath. They
  are draconic, so Dragon hunter weapons and dragon bolts (e) apply.
- The installed plugin version is now shown in the side-panel footer (e.g.
  "Slayer Loadout v1.4.0").

### Changed
- Enchanted dragon bolts (Ruby/Diamond dragon bolts (e)) are now preferred on any
  draconic task (the "dragon" attribute), not just bosses, so crossbows on dragons and
  wyverns use dragon bolts over broad bolts when owned.

## [1.3.0] - 2026-05-24

Released via [runelite/plugin-hub#12163](https://github.com/runelite/plugin-hub/pull/12163).

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
