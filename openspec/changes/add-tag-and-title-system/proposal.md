## Why

The repository is empty and needs its first functional release. Server owners want cosmetic identity for their players — a **tag** (a rank/cosmetic prefix on chat, nametag and tab list) and a **title** (a second, independent line floating above the nametag that advertises an achievement, such as a tournament win). Existing solutions either hard-couple both concepts into one prefix, require a paid permissions plugin to author entries, or force a restart to add a single tag. `s-tags` 0.1.0 delivers both concepts as separate, independently selectable cosmetics that an operator can author entirely in-game.

## What Changes

- Introduce the `s-tags` Gradle (Kotlin DSL) project targeting **Java 25** and **Paper API 26.2**, packaged as a Paper plugin (`paper-plugin.yml`, `api-version: '26.2'`), released as version **0.1.0**.
- Add a **tag** cosmetic: an operator-defined entry with an id, an Adventure-formatted prefix, a permission node, a weight and selector metadata. A player owns every tag whose permission they hold and chooses which one is active.
- Render the active tag in **chat**, on the **nametag above the head** (scoreboard team prefix) and in the **tab list**, each independently toggleable.
- Add a **title** cosmetic: the same authoring shape as a tag (id, prefix, permission, weight), but rendered as a **per-viewer hologram** above the nametag using a vanilla `TextDisplay` entity, shown selectively via `Player#showEntity` / `hideEntity`. Tag and title are selected independently.
- Add a **selector** for both cosmetics, available as an **inventory GUI** or a **chat-based** selector, chosen in `config.yml`. GUI layout (size, slots, filler, item material, sorting, pagination) is configurable.
- Add **in-game interactive authoring**: a conversational, step-by-step flow that lets an operator create, edit, preview and delete tags and titles without touching a file or restarting the server.
- Add a **pluggable persistence layer** with two interchangeable backends selected in `config.yml`: **MySQL** (HikariCP pool, indexed schema, versioned migrations, fully asynchronous) and **YAML** (single file, debounced batched writes). Both are fronted by an in-memory cache so no lookup on the main thread ever performs I/O.
- Add full **externalized configuration**: `config.yml` for behavior and `messages.yml` for every player-facing string, with MiniMessage formatting and an in-game reload.
- Add a **PlaceholderAPI** integration (soft dependency): expose `%stags_*%` placeholders and resolve placeholders embedded in tag/title prefixes when PlaceholderAPI is present.
- Add the **GitFlow** branching model, Conventional Commits, a GitHub Actions build workflow, issue/PR templates, `README.md`, `CHANGELOG.md`, `LICENSE` and `CONTRIBUTING.md` to the already-provisioned GitHub repository.

## Capabilities

### New Capabilities

- `tag-management`: definition, validation and lifecycle of tags; permission-derived ownership; active-tag selection, fallback and precedence rules.
- `title-management`: definition, validation and lifecycle of titles; permission-derived ownership; active-title selection, independent from the tag.
- `nameplate-rendering`: where and how the active cosmetics are displayed — chat message format, scoreboard-team nametag prefix, tab list entry, and the per-viewer `TextDisplay` hologram carrying the title.
- `cosmetic-selector`: the player-facing selector for choosing an active tag or title, in either GUI or chat mode, including pagination, locked-entry presentation and clearing a selection.
- `in-game-authoring`: the interactive, step-by-step operator flow for creating, editing, previewing and deleting tags and titles at runtime.
- `persistence`: the storage contract and its MySQL and YAML backends, including caching, asynchronous access, schema migration and failure behavior.
- `plugin-configuration`: `config.yml` and `messages.yml` structure, defaults, validation, MiniMessage message rendering and runtime reload.
- `placeholder-integration`: the PlaceholderAPI expansion this plugin provides and the placeholder resolution it consumes inside cosmetic prefixes.

### Modified Capabilities

None — this is the first change in the project.

## Impact

- **New codebase**: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, Gradle wrapper, and a `com.github.viniciussambinello.stags` source tree split into `domain`, `application` and `infrastructure` layers per the project's clean-architecture convention.
- **Runtime dependencies**: `io.papermc.paper:paper-api:26.2.build.+` (compile-only), HikariCP + MySQL Connector/J (shaded and relocated), and PlaceholderAPI as a compile-only soft dependency.
- **Toolchain**: Java 25 toolchain; Paper 26.2 ships unobfuscated, so `paperweight-userdev` is not required — plain `paper-api` plus the Shadow plugin suffices.
- **Server-side effects**: the plugin claims scoreboard teams for nametag prefixes and spawns one `TextDisplay` passenger per player carrying a title. Both are opt-out via configuration to avoid conflicting with an existing nametag or hologram plugin.
- **Chat**: when chat formatting is enabled the plugin renders `AsyncChatEvent`; this conflicts with any other plugin that also renders chat, so it defaults to enabled but is documented as the first thing to disable on conflict.
- **Database**: creates and owns the `stags_player_selection` and `stags_cosmetic` tables (configurable prefix) when the MySQL backend is active.
- **Repository**: adds the GitFlow `develop` branch and branch-protection expectations to `https://github.com/ViniciusSambinello/s-tags`.

## Non-goals

- No per-world, per-server or time-limited (expiring) cosmetics in 0.1.0.
- No economy, purchase, crate or reward integration — cosmetics are granted by permission only.
- No Vault dependency; permissions are read through the Bukkit permission API only.
- No multi-line holograms, no animated/scrolling titles, no per-viewer *content* variation beyond visibility (every viewer that can see a title sees the same text).
- No cross-server synchronization, Redis pub/sub or proxy (Velocity/BungeeCord) component.
- No migration tooling from other tag plugins, and no import/export command.
- No support for legacy `§`/`&` colour codes as the authoring format; MiniMessage is the single supported format (a legacy string is accepted only as a documented convenience during in-game authoring and stored normalized as MiniMessage).
- No Spigot or Folia support in 0.1.0 — Paper 26.2 only.
