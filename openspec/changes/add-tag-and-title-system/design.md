## Context

Greenfield repository — only `README.md`, `.git` and `openspec/` exist. See `proposal.md` — Why for motivation and `specs/` for the behavior contract.

Platform constraints that shape everything below:

- **Paper 26.2 ships unobfuscated.** Mojang removed server-JAR obfuscation as of 26.1, so Paper dropped reobfuscation from dev bundles. `paperweight-userdev` is therefore unnecessary unless we touch server internals — and we deliberately do not. Plain `paper-api` plus Shadow is the whole build.
- **Adventure 5** is bundled with Paper 26.2. `Component`, `MiniMessage` and `ClickEvent.callback` are available from the server classpath; we must not shade Adventure.
- **Java 25** is required to compile against Paper 26.2 and is the language level for the plugin.
- **One server thread.** Every JDBC statement, every file write and every MiniMessage parse of an operator-supplied string is a main-thread hazard. The architecture is built around never letting storage touch the tick loop.
- **Immutability is a hard project convention**, and so is the absence of code comments. Both push design toward records, sealed interfaces and snapshot-swapping over in-place mutation, because self-documenting types are the only documentation the code is allowed to have.

## Goals / Non-Goals

**Goals:**

- A domain layer with zero `org.bukkit` / `io.papermc` imports, so tag and title rules are unit-testable without a server.
- Storage backends interchangeable behind one port, with identical observable behavior, chosen by one config value.
- Zero storage I/O on the main thread and zero storage reads after startup for catalogue lookups.
- Rendering driven by events, not by a repeating task.
- All state either immutable or confined to a small number of explicitly-owned concurrent holders.

**Non-Goals:**

- No dependency-injection framework. Constructor wiring is done by hand in a single composition root; a container would be more machinery than a plugin of this size justifies.
- No ORM. The query surface is five statements; JDBC with prepared statements is smaller, faster and has no reflection cost at startup.
- No abstraction over Bukkit "just in case" beyond the ports the domain actually needs. Ports exist where a second implementation exists or is specified.
- No NMS, no packet library, no reflection into server internals.

## Decisions

### D1 — Build: Gradle Kotlin DSL, version catalog, Shadow, no paperweight

`settings.gradle.kts` + `build.gradle.kts` + `gradle/libs.versions.toml`. Java toolchain 25, release 25.

```
compileOnly("io.papermc.paper:paper-api:26.2.build.+")
compileOnly("me.clip:placeholderapi:<version>")
implementation("com.zaxxer:HikariCP:<version>")
implementation("com.mysql:mysql-connector-j:<version>")
```

Group id `io.github.viniciussambinello`, root package `io.github.viniciussambinello.stags`, version `0.1.0`.

- **Why `compileOnly` for paper-api:** the server provides it; bundling it would break the plugin.
- **Why Shadow with relocation:** HikariCP and Connector/J must be relocated under `io.github.viniciussambinello.stags.libs` so we never collide with another plugin's copy or with a server-provided driver. This is the single most common source of "works on my server" breakage.
- **Why a version catalog:** the dependency set is small but the *versions* are the volatile part across Minecraft drops; one file to bump.
- **Why not `paperweight-userdev`:** it exists to map against obfuscated internals. Paper 26.2 has no obfuscation and we use no internals, so it would add build cost and a dev-bundle download for nothing.
- **Alternative rejected — runtime library downloading** via the Paper plugin loader's `PluginClasspathBuilder`. It keeps the jar small and is idiomatic on modern Paper, but it makes a first startup depend on Maven Central reachability. For 0.1.0 a self-contained ~4 MB jar is the safer default. Revisit in a later version.

### D2 — Paper plugin format with a bootstrapper

`paper-plugin.yml` with `api-version: '26.2'`, a `PluginBootstrap` and a `JavaPlugin`.

The bootstrapper is where configuration is read and the composition root is built, so that `createPlugin(PluginProviderContext)` can hand a **fully constructed, final** plugin instance its collaborators through a constructor. This is what makes "no static singletons, no service locator, all fields final" actually achievable — with the legacy `plugin.yml` format the server constructs the plugin with a no-arg constructor and every collaborator has to be assigned in `onEnable`, which forces non-final fields.

- **Alternative rejected — legacy `plugin.yml`:** wider server compatibility, but we target Paper 26.2 only and it would cost us the immutability property above.

### D3 — Layering and package layout

```
io.github.viniciussambinello.stags
├── domain              no Bukkit, no JDBC, no Adventure-outside-Component
│   ├── cosmetic        Cosmetic, CosmeticKind, CosmeticId, Weight, Prefix
│   ├── player          PlayerCosmetics, Selection (sealed), CosmeticOwnership
│   └── catalogue       Catalogue (immutable snapshot), CatalogueRules
├── application         use cases, ports, orchestration
│   ├── port            CosmeticRepository, SelectionRepository, PermissionOracle,
│   │                   CosmeticRenderer, MessageSource, Clock
│   ├── usecase         SelectCosmetic, ClearCosmetic, ResolveActiveCosmetic,
│   │                   CreateCosmetic, EditCosmetic, DeleteCosmetic, LoadPlayer
│   └── service         CatalogueService, PlayerCosmeticService, AuthoringService
└── infrastructure      every adapter
    ├── bootstrap       StagsBootstrap, StagsPlugin, composition root
    ├── config          ConfigLoader, StagsConfig (record tree), MessageCatalog
    ├── storage
    │   ├── mysql       MySqlCosmeticRepository, MySqlSelectionRepository,
    │   │               HikariConnectionProvider, SchemaMigrator
    │   └── yaml        YamlCosmeticRepository, YamlSelectionRepository, DebouncedWriter
    ├── render          ChatRenderAdapter, NametagRenderAdapter,
    │                   TabListRenderAdapter, TitleHologramRenderAdapter
    ├── selector        MenuSelector, ChatSelector, MenuLayout, MenuHolder
    ├── authoring       ChatAuthoringFlow, AuthoringSessionStore, AuthoringStep
    ├── command         StagsCommand, AdminCommand, Brigadier wiring, completions
    ├── placeholder     StagsExpansion, PlaceholderResolver + NoopResolver
    └── concurrent      StorageExecutor, MainThreadDispatcher
```

Dependencies point inward only. `domain` compiles with nothing but the JDK. `application` depends on `domain` and its own ports. `infrastructure` depends on both and on Paper.

The one deliberate concession: `domain` holds Adventure `Component` values for rendered prefixes. Adventure is a rendering-agnostic text model, not a server API, and the alternative — a hand-rolled text type converted at every boundary — would add a whole parallel model for no gain.

### D4 — Immutability strategy

- Every value type is a `record`: `CosmeticId`, `Weight`, `Prefix`, `Cosmetic`, `PlayerCosmetics`, and the entire `StagsConfig` tree.
- `Selection` is a **sealed interface** with three permitted records — `Unset`, `Active(CosmeticId)`, `Cleared` — which makes the three-state storage semantics from `persistence` a compile-time exhaustive `switch` rather than a nullable field plus a boolean. This is the type that most directly encodes a spec requirement.
- Collections entering a record are `List.copyOf` / `Map.copyOf` in the compact constructor; nothing hands out a mutable view.
- The catalogue is an **immutable snapshot swapped atomically**: `AtomicReference<Catalogue>`, where `Catalogue` holds pre-sorted lists and an id-keyed map built once. A mutation builds a new `Catalogue` from the old one and CASes it in. Readers on the main thread get a consistent snapshot with no lock and no allocation.
- Per-player state lives in exactly one mutable structure — `ConcurrentHashMap<UUID, PlayerCosmetics>` — whose *values* are immutable records replaced with `compute`. Mutation is confined to that one map and to the authoring session store.

**Alternative rejected — a fully persistent/functional map library.** Copy-on-write of a catalogue holding tens to low hundreds of entries, mutated only by an operator command, is trivially cheap; a persistent-collections dependency would buy nothing at this scale.

### D5 — Concurrency model

- One `StorageExecutor`: a single-threaded executor with a named thread. Single-threaded is deliberate — it serializes writes, which is what makes "exactly one creation succeeds" in the authoring race scenario fall out of the design rather than needing a lock.
- All port methods that touch storage return `CompletableFuture<T>` and never run on the caller's thread.
- `MainThreadDispatcher` wraps the Bukkit scheduler; every result that mutates gameplay state hops back through it. Rendering only ever happens on the main thread.
- Identifier uniqueness is enforced *at commit time* on the storage thread, in addition to the input-step check — that is what closes the two-operator race.
- Shutdown: the executor is drained with a bounded await, the YAML debouncer force-flushes, then holograms and teams are torn down.

### D6 — Storage: one port pair, two adapters

`CosmeticRepository` (catalogue CRUD) and `SelectionRepository` (per-player state) are the only storage ports. Both backends implement both.

**MySQL.** HikariCP pool (relocated), configurable size, `INSERT ... ON DUPLICATE KEY UPDATE` for upserts.

```
<prefix>schema_version(version INT PRIMARY KEY, applied_at TIMESTAMP)
<prefix>cosmetic(kind VARCHAR(8), id VARCHAR(32), prefix VARCHAR(255),
                 permission VARCHAR(128), weight INT,
                 PRIMARY KEY (kind, id))
<prefix>player_selection(player_uuid BINARY(16), kind VARCHAR(8),
                         cosmetic_id VARCHAR(32) NULL, cleared BOOLEAN,
                         PRIMARY KEY (player_uuid, kind))
```

- `BINARY(16)` for the UUID rather than `CHAR(36)`: 16 bytes instead of 36, and it keeps the primary key compact so the per-join lookup is a single clustered-index hit. This is the hot path — one query per join.
- Composite primary keys mean no secondary index is needed for either access pattern; the spec's "at most one row per cosmetic kind" is enforced by the key itself.
- `cosmetic_id NULL` + `cleared` encodes the sealed `Selection` faithfully.
- `SchemaMigrator` applies ordered forward migrations and refuses to start on a future version.

**YAML.** State held in memory, mirrored to `cosmetics.yml` and `selections.yml`. `DebouncedWriter` coalesces changes over a configurable interval, writes to a temp file and does an atomic move (`ATOMIC_MOVE`, falling back to `REPLACE_EXISTING`) so an interrupted write cannot corrupt the live file.

- **Alternative rejected — SQLite as the file backend.** Better concurrency semantics than YAML, but the user asked specifically for a human-editable `.yml` fallback, and hand-editability is the whole point of that backend.
- **Alternative rejected — a file per player.** Simpler write amplification story, but it turns a restart into thousands of file opens. A single debounced file is faster to load and easier to back up.

### D7 — Rendering

**Chat** — `AsyncChatEvent` with a `ChatRenderer`. Rendering happens on the async chat thread reading immutable snapshots, so it costs the main thread nothing. The message body is inserted as a literal `Component` unless the sender holds the formatting permission, which is what makes the "markup rendered literally" scenario safe by construction rather than by sanitizing strings.

**Nametag** — scoreboard teams, **one team per tag** rather than one per player. With N players and M tags this is M teams instead of N, and a player changing tags is one team-membership packet instead of a team create/destroy. Team names are generated as `st_` + a short deterministic hash of the cosmetic id, bounded to a safe length. Unlike the tab list, the floating nametag has no ordering concept to encode into the name: each player's nametag renders independently above their own head from their own team's prefix/color, not as a sorted list, and team-name-based sorting only ever affects the tab list's legacy fallback order, which Paper's `Player#setPlayerListOrder` (used directly, see below) makes irrelevant here.

**Tab list** — `Player#playerListName(Component)` for the prefix and Paper's player list order API for weight-based ordering.

**Title hologram** — a `TextDisplay` spawned and added as a **passenger of the player**, with the vertical offset applied through the display's transformation translation. Visibility is per-viewer via `Player#hideEntity` / `showEntity`, which is what satisfies self-visibility and the "hidden player" scenario without any packet work.

- **Why a passenger over a follow task:** riding gives client-side interpolation, so the hologram is smooth and correct through movement, teleports and world changes with zero server-side per-tick cost. A teleport-follow task would be a scheduled task per player per tick, and would still look worse.
- **Alternative rejected — packet-level virtual entities** (ProtocolLib or a packet-events library). Strictly more control and no real entity, but it adds a hard external dependency, breaks on protocol changes every drop, and per-viewer *visibility* — all we actually need — is already a first-class Bukkit API.

Every hologram is stamped with a persistent-data key identifying it as ours plus the owner UUID, which is what makes orphan cleanup on startup and chunk load possible after an unclean shutdown.

**Cost of prefix parsing.** MiniMessage is parsed **once**, when a cosmetic is loaded or edited, and the resulting `Component` is stored in the immutable `Cosmetic` record. A cosmetic whose prefix contains no placeholder is rendered straight from that cached `Component`; only prefixes flagged as placeholder-bearing at load time re-resolve per render. This is the decision that keeps chat and tab rendering allocation-light.

### D8 — Selector

One `Selector` port, two adapters chosen by config.

- **Menu** — the inventory is built from an immutable `MenuLayout` record parsed from config. Click handling is identified by an `InventoryHolder` marker implementation, not by comparing inventory titles, so a renamed menu never breaks click routing and no other plugin's inventory can be mistaken for ours. Pagination is computed from the layout's content slots.
- **Chat** — Adventure `ClickEvent.callback(...)`, which binds a click to a server-side callback with an expiry instead of to a command. This avoids publishing an internal "select" command that players could invoke directly to bypass the selector's cooldown, and it means no hidden command surface exists at all.

Cooldowns are held in a `ConcurrentHashMap<UUID, Instant>` checked before any use case is invoked, so a rate-limited request never reaches the storage executor.

### D9 — In-game authoring as an immutable state machine

An authoring flow is a sealed `AuthoringStep` progression over an immutable `AuthoringSession` record. Each input produces a **new** session; nothing is mutated in place. Sessions live in `ConcurrentHashMap<UUID, AuthoringSession>` with a scheduled sweep for the configured timeout, and are removed on quit.

Input is captured by intercepting `AsyncChatEvent` for players that hold an active session and cancelling it, so the flow never leaks into public chat. Validation at each step calls the *same* `CatalogueRules` the repository enforces at commit — one implementation of the rules, two call sites, which is why step-level and commit-time validation cannot drift.

Preview at the confirmation step renders the candidate cosmetic to that operator only; the title preview spawns a viewer-scoped display that is removed when the step ends, so no persistent state is created.

- **Alternative rejected — Bukkit's `Conversation` API.** It predates Adventure, is string-and-`ChatColor`-oriented, and its `Prompt` contract is inherently mutable. Rolling a small sealed state machine is less code than adapting it and matches the project's immutability rule.

### D10 — Commands via the Paper Brigadier API

Commands are registered through the `LifecycleEvents.COMMANDS` lifecycle event using Paper's Brigadier `Commands` API, not through `plugin.yml` command blocks. This gives typed arguments, per-node permission gating and tab completion from one tree definition — the spec's completion and usage requirements come from the tree rather than from hand-written `onTabComplete` branching.

### D11 — PlaceholderAPI isolation

PlaceholderAPI is `compileOnly`. All direct references live in two classes behind a `PlaceholderResolver` port with a no-op implementation. Nothing outside `infrastructure.placeholder` references a PlaceholderAPI type, so the classes are never loaded when the plugin is absent — that is what makes "enables normally with no error" true rather than merely caught. Presence is detected at enable and on `PluginEnableEvent`, so a later-loading PlaceholderAPI still activates the integration.

Whether a prefix contains a placeholder is determined once at load/edit time and stored as a flag on the `Cosmetic`; resolution is a single non-recursive pass.

### D12 — Git and release process

GitFlow: `main` (tagged releases only), `develop` (integration), `feature/*`, `release/*`, `hotfix/*`. Conventional Commits. Feature branches merge into `develop` via PR; a `release/0.1.0` branch stabilizes and merges to both `main` and `develop`, tagged `v0.1.0`.

CI on GitHub Actions: JDK 25, `./gradlew build` on push and PR to `develop`/`main`; a tag push additionally publishes the shaded jar as a release asset.

## Risks / Trade-offs

- **Chat rendering conflicts with another chat plugin** → the chat target is independently toggleable and documented as the first thing to disable; the plugin renders rather than rewriting the format string, so it composes better than a `setFormat` approach but still cannot coexist with a second renderer.
- **Scoreboard team conflicts** with a plugin that also owns nametags → we only ever create, modify and remove teams under our own `st_` prefix, never touch foreign teams, and fully unregister ours on disable. The nametag target is toggleable for servers where another plugin must win.
- **`TextDisplay` as a player passenger has side effects** — the player technically has a passenger, which a third-party plugin inspecting passengers may not expect, and vehicle interactions carry the display along → holograms are tagged in persistent data so ours are identifiable and skippable, the target is toggleable, and cleanup runs on startup and chunk load. Accepted as the cost of getting free client-side interpolation.
- **Entity count** — one extra entity per player with an active title → displays are non-ticking as far as gameplay is concerned (no gravity, no collision, no AI); the cost is packet volume in dense areas, bounded by vanilla entity render distance.
- **Shading Connector/J and Hikari grows the jar and can conflict** if the server also provides a driver → both are relocated under our own package, which is the standard mitigation; the YAML backend is available for operators who want a zero-dependency install.
- **Java 25 is not yet universal on shared hosts** → this is inherent to targeting Paper 26.2, which itself requires JDK 25. Documented as a hard requirement in the README rather than mitigated.
- **Single-threaded storage executor is a throughput ceiling** → correct trade for a plugin whose write volume is human-paced; it buys serialized writes and a race-free commit path. If it ever becomes a bottleneck the executor is one constructor argument away from being a pool, with the commit-time uniqueness check moved to a database constraint (which the composite primary key already provides).
- **`AtomicReference` catalogue swap is last-write-wins** if two operators edit different fields of the same cosmetic concurrently → all catalogue mutations are serialized on the single storage thread and read-modify-write happens there, so the interleaving that would lose an edit cannot occur.
- **`ClickEvent.callback` entries expire** → the chat selector's callbacks are given an explicit expiry aligned with the selector cooldown, and an expired click falls through to the configured "expired, reopen the selector" message.

## Migration Plan

No migration — this is the first release of a new plugin.

**Deploy:** `./gradlew shadowJar` → drop the jar in `plugins/` → start on Paper 26.2 with JDK 25 → the plugin writes `config.yml` and `messages.yml` on first run and defaults to the YAML backend, so an operator gets a working install with no database.

**Enabling MySQL:** set the backend to `MYSQL`, fill in credentials, restart. Switching backends is restart-only by design (see `plugin-configuration`); 0.1.0 ships no data migration between backends, so switching starts from an empty catalogue in the new backend.

**Rollback:** remove the jar. YAML files and MySQL tables are left intact and are forward-compatible with a reinstall. Because the plugin removes its scoreboard teams and holograms on disable, a rollback leaves no residue on the server.

## Open Questions

- Which exact HikariCP and MySQL Connector/J versions to pin in the version catalog — resolved during the build-setup task against what is current at implementation time; does not affect specs, approach or task breakdown.
- Whether the shipped default menu layout should be a 6-row or 3-row inventory — a cosmetic default, changeable in config by any operator.
