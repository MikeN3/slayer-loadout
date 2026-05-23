# Changelog

All notable changes to **Slayer Loadout** are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
This plugin is distributed via the RuneLite Plugin Hub, which pins a specific commit
rather than a version tag, so version numbers below are a human-readable convenience.

## [Unreleased]

In review via [runelite/plugin-hub#12157](https://github.com/runelite/plugin-hub/pull/12157).

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
