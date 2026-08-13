# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.1] - 2026-08-13

### Added

- Legacy `&`-style color codes (for example `&cSomething`) in cosmetic
  prefixes are now detected and automatically converted to MiniMessage,
  so they render correctly instead of showing as literal, uncolored text.
  Prefixes already written in MiniMessage are unaffected.

### Fixed

- The tab list crashed with `IllegalArgumentException: order cannot be
  negative` for any player with no active tag while tab list ordering was
  set to `WEIGHT` — triggered on join, and on deleting a tag the player
  had equipped.
- A tag or title could be authored with a negative weight, which would
  reproduce the same tab list crash the moment it became a player's
  active cosmetic. Negative weights are now rejected at authoring time
  with a clear message instead of being silently accepted.

## [0.1.0] - 2026-08-13

### Added

- Tag system: operator-defined chat/nametag/tab-list prefixes, owned through
  Bukkit permissions, with player-driven active selection and automatic
  weight-based fallback.
- Title system: an independently selectable per-viewer hologram rendered
  above a player's nametag, sharing the tag authoring model but with no
  automatic default.
- Configurable cosmetic selector, available as an inventory menu or a
  chat-based list.
- In-game interactive authoring flow for creating, editing, previewing and
  deleting tags and titles without a restart.
- Pluggable persistence: MySQL (HikariCP, indexed schema, async access) or a
  single debounced YAML file, selected in `config.yml`.
- PlaceholderAPI soft integration: published `%stags_*%` placeholders and
  placeholder resolution inside cosmetic prefixes.
- Fully externalized `config.yml` and `messages.yml`, both reloadable at
  runtime.

[Unreleased]: https://github.com/ViniciusSambinello/s-tags/compare/v0.1.1...HEAD
[0.1.1]: https://github.com/ViniciusSambinello/s-tags/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/ViniciusSambinello/s-tags/releases/tag/v0.1.0
