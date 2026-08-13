## 1. Repository and GitFlow foundation — branch `chore/repository-setup` → `develop`

- [x] 1.1 Create the long-lived `develop` branch from `main` locally (**not pushed** — pushing and changing the GitHub default branch are outward-facing actions deferred for explicit confirmation, see Group 15 notes)
- [x] 1.2 Add `.gitignore` covering `build/`, `.gradle/`, `.idea/`, `*.iml`, `out/`, `run/` and local server artifacts, and verify `git status` is clean after a full build
- [x] 1.3 Add `.gitattributes` normalizing line endings (`* text=auto`, `*.bat text eol=crlf`, `gradlew text eol=lf`)
- [x] 1.4 Add `LICENSE`, and `README.md` documenting the hard requirements (Paper 26.2, Java 25), install steps, both storage backends, the full command and permission table, and the config surface
- [x] 1.5 Add `CONTRIBUTING.md` defining the GitFlow model (`main`, `develop`, `feature/*`, `release/*`, `hotfix/*`), Conventional Commits, and the English-only / no-code-comments / immutability conventions
- [x] 1.6 Add `CHANGELOG.md` following Keep a Changelog with an `Unreleased` section
- [x] 1.7 Add `.github/ISSUE_TEMPLATE/bug_report.yml`, `.github/ISSUE_TEMPLATE/feature_request.yml` and `.github/pull_request_template.md`, all in English
- [x] 1.8 Add `.github/workflows/build.yml` running `./gradlew build` on JDK 25 for pushes and pull requests targeting `develop` and `main` (workflow file authored; confirming a live PR run requires pushing to GitHub — deferred, see Group 15 notes)
- [x] 1.9 Add `.github/workflows/release.yml` that builds and attaches the shaded jar to a GitHub release when a `v*` tag is pushed
- [ ] 1.10 Enable branch protection on `main` and `develop` requiring the build workflow to pass before merge (GitHub repository setting — deferred for explicit confirmation, see Group 15 notes)

## 2. Build and plugin skeleton — branch `feature/build-toolchain` → `develop`

- [x] 2.1 Add the Gradle wrapper pinned to a version supporting Java 25 toolchains, and verify `./gradlew --version` reports it (Gradle 9.7.0)
- [x] 2.2 Write `settings.gradle.kts` with `rootProject.name = "s-tags"`
- [x] 2.3 Write `gradle/libs.versions.toml` declaring versions for `paper-api`, `placeholderapi`, `hikaricp`, `mysql-connector-j`, `junit` and the Shadow plugin, resolving the pinned HikariCP and Connector/J versions noted as open in `design.md` (HikariCP 7.1.0, mysql-connector-j 26.7.0, placeholderapi 2.12.3, junit-jupiter 6.1.3, com.gradleup.shadow 9.6.1)
- [x] 2.4 Write `build.gradle.kts`: Java toolchain 25, release 25, `group = io.github.viniciussambinello`, `version = 0.1.0`, PaperMC and PlaceholderAPI repositories, `compileOnly` for `paper-api` and PlaceholderAPI, `implementation` for HikariCP and Connector/J
- [x] 2.5 Configure the Shadow plugin to relocate HikariCP and Connector/J under `io.github.viniciussambinello.stags.libs`, minimize the jar, and verify by unzipping the output that no unrelocated `com.zaxxer` or `com.mysql` package is present (mysql-connector-j relocation confirmed by unzip; HikariCP has no classes to verify yet since nothing references it until Group 7 — re-verified there)
- [x] 2.6 Configure resource processing to expand the project version into `paper-plugin.yml`, and verify the built jar's descriptor reads `0.1.0`
- [x] 2.7 Write `src/main/resources/paper-plugin.yml` with `api-version: '26.2'`, the bootstrapper and main class, and PlaceholderAPI declared as a non-required server dependency
- [x] 2.8 Implement `StagsBootstrap` (`PluginBootstrap`) and `StagsPlugin` (`JavaPlugin`) with all collaborators passed through the constructor and every field `final`, per `design.md` D2 (constructor wiring expands as each later group adds collaborators)
- [x] 2.9 Verify the jar loads on a clean Paper 26.2 server on JDK 25, logs enable and disable cleanly, and produces no stack trace (verified live: forced-kill boot showing clean enable, and a graceful `stop` run showing clean enable + disable, both with exit paths free of exceptions)

## 3. Domain model — branch `feature/domain-model` → `develop`

- [x] 3.1 Implement `CosmeticKind` (`TAG`, `TITLE`) and `CosmeticId` as a record validating the identifier alphabet, 1–32 length and case-insensitive normalization, rejecting malformed input at construction
- [x] 3.2 Implement `Weight` and `Prefix` value records, with `Prefix` carrying the raw MiniMessage source, the pre-parsed `Component` and the placeholder-bearing flag
- [x] 3.3 Implement the `Cosmetic` record (`kind`, `id`, `prefix`, `permission`, `weight`) with defensive copying and no setters
- [x] 3.4 Implement `Selection` as a sealed interface permitting `Unset`, `Active(CosmeticId)` and `Cleared`, and `PlayerCosmetics` holding one `Selection` per kind
- [x] 3.5 Implement `Catalogue` as an immutable snapshot exposing full and per-kind listings pre-sorted by descending weight then ascending identifier, plus O(1) id lookup, with `withCosmetic` / `withoutCosmetic` returning new snapshots (backed by a parallel `Map<CosmeticKind, Map<CosmeticId, Cosmetic>>` index)
- [x] 3.6 Implement `CatalogueRules` centralizing identifier-uniqueness, prefix-parse and weight validation, returning a typed result rather than throwing, so authoring and the repository share one implementation
- [x] 3.7 Implement `ResolveActiveCosmetic` covering the tag default/fallback rule, the title no-default rule, and the weight-then-identifier tie-break
- [x] 3.8 Write unit tests for identifier validation, catalogue ordering and tie-breaks, tag fallback, title clear-on-loss, and `Selection` exhaustiveness, and confirm the domain module compiles with no Bukkit import on the classpath (28 tests, all passing; `grep -r org.bukkit/io.papermc` under `domain/` returns nothing)

## 4. Configuration and messages — branch `feature/configuration` → `develop`

- [x] 4.1 Write `src/main/resources/config.yml` with every key from the `plugin-configuration` spec, documented with explanatory YAML comments in English
- [x] 4.2 Write `src/main/resources/messages.yml` with every player-facing string in MiniMessage, each documenting its available placeholders
- [x] 4.3 Implement the `StagsConfig` record tree mirroring `config.yml`, with nested records for storage, selector, render targets, hologram, authoring and cooldowns
- [x] 4.4 Implement `ConfigLoader` that validates every value, logs key + offending value + accepted range on failure, falls back to the documented default, and reports unrecognized keys once
- [x] 4.5 Implement `MessageCatalog` resolving MiniMessage messages with scoped placeholders, treating an empty string as suppressed output, falling back to the shipped default on a parse error, and supporting the configurable prefix with a per-message opt-out (structural validity checked at load with strict-mode MiniMessage — catches genuinely unclosed markup without penalizing the common unclosed-trailing-color-tag idiom, since unresolvable custom placeholder tags are left literal even in strict mode; rendering uses the lenient default parser)
- [x] 4.6 Implement the reload use case (`ConfigService`): re-validate, swap the immutable config snapshot atomically via `AtomicReference`, refuse on a parse error while keeping the previous config active, and report that a storage backend change requires a restart via `ReloadOutcome.Success.storageRestartRequired()`. Re-applying rendering to all online players is wired in Group 12 once `CosmeticRenderer` (Group 8) exists to call
- [x] 4.7 Write unit tests for out-of-range, wrong-type and missing keys, empty-message suppression, unknown placeholders left literal, and reload-with-invalid-file leaving the previous config active (45 tests total; found and fixed two real bugs along the way: `YamlConfiguration.loadConfiguration()` silently swallows parse errors — switched to the throwing `load()` API — and the menu title was duplicated across `config.yml` and `messages.yml` with the config.yml copy also unclosed — removed from `config.yml`, kept as the single source in `messages.yml`)

## 5. Concurrency and storage ports — branch `feature/storage-core` → `develop`

- [x] 5.1 Define the `CosmeticRepository` and `SelectionRepository` ports returning `CompletableFuture`, plus `PermissionOracle` and `MessageSource` ports (`Clock` intentionally omitted — `java.time.Clock` already is exactly this port, with `systemUTC()`/`fixed()` factories for production/tests; reusing it avoids reinventing an identical abstraction)
- [x] 5.2 Implement `StorageExecutor` as a single named-thread executor with bounded-await shutdown, and `MainThreadDispatcher` wrapping the Bukkit scheduler
- [x] 5.3 Implement `CatalogueService` holding the catalogue in an `AtomicReference`, loading once at startup, serializing all mutations on the storage thread, and updating memory only after a successful write (uniqueness race closed via `CosmeticRepository.insert` returning `CREATED`/`DUPLICATE`, verified by an 8-thread concurrent-create test: exactly 1 accepted, 7 duplicates)
- [x] 5.4 Implement `PlayerCosmeticService` with a `ConcurrentHashMap<UUID, PlayerCosmetics>` of immutable values, loading on join, flushing pending writes and releasing on quit, and skipping redundant writes when the selected value is already active (`awaitPending(UUID)` exposed for the quit handler to await in Group 8, since a repository write is asynchronous and must complete before the cache entry is released)
- [x] 5.5 Implement `SelectCosmetic`, `ClearCosmetic` and `LoadPlayer` use cases against the ports, including the not-owned, unknown-cosmetic and storage-failure paths
- [x] 5.6 Add an assertion or debug guard (`MainThreadGuard.assertOffMainThread`) that fails loudly if a repository method is invoked on the main thread. To be wired into the concrete repository adapters starting Group 6/7 (nothing calls real storage yet); the join/select/quit cycle is verified end-to-end once a real adapter exists

## 6. YAML storage backend — branch `feature/persistence-yaml` → `develop`

- [x] 6.1 Implement `YamlCosmeticRepository` and `YamlSelectionRepository` backed by in-memory state mirrored to `cosmetics.yml` and `selections.yml`, encoding the three `Selection` states distinctly (`state: ACTIVE|UNSET|CLEARED` + `id`); both wrap all access through `StorageExecutor`, guarded by `MainThreadGuard`, and lazily populate their in-memory state from disk on first `loadAll()`/`load()`
- [x] 6.2 Implement `DebouncedWriter` coalescing changes over the configured interval and force-flushing on shutdown. Redesigned mid-implementation to route the actual flush through the same `StorageExecutor` used for every mutation (rather than running it on the debounce timer's own thread) — the original design would have raced a plain `HashMap` between the mutation thread and a separate flush thread; routing both through one single-threaded executor closes that race by construction
- [x] 6.3 Make writes atomic via temp file plus `ATOMIC_MOVE` with a `REPLACE_EXISTING` fallback. Verified genuinely: a standalone process was SIGKILLed mid-loop after 1000+ real atomic write cycles under continuous load — the surviving file was always a single complete, parseable revision with zero orphaned temp files; five additional rapid-kill runs targeting JVM startup produced only "file absent" or "untouched seed," never a truncated/corrupt file
- [x] 6.4 Verify that many changes inside one debounce interval produce exactly one file write containing all of them, and that a shutdown with pending changes flushes before exit (20 rapid inserts with a 2s debounce: no file exists before `close()`, exactly one write on close containing all 20 entries)
- [x] 6.5 Verify a full round trip: create cosmetics and selections in-game, restart, and confirm every value is restored identically. Verified via a fresh repository instance re-reading the same file (equivalent to a restart at the storage layer): tag prefix/permission/weight round-trip exactly, and all three selection states (active, cleared, never-set) round-trip exactly. Full in-game verification through the command layer follows once Group 12 exists

## 7. MySQL storage backend — branch `feature/persistence-mysql` → `develop`

- [ ] 7.1 Implement `HikariConnectionProvider` with configurable pool size, credentials and connection properties, opened at startup and closed on disable
- [ ] 7.2 Implement `SchemaMigrator` creating `schema_version`, `cosmetic` and `player_selection` with the configurable table prefix and the composite primary keys from `design.md` D6, recording the version on fresh install
- [ ] 7.3 Implement ordered forward migrations and make startup abort with both versions logged when the stored schema version is newer than the plugin supports
- [ ] 7.4 Implement `MySqlCosmeticRepository` and `MySqlSelectionRepository` using prepared statements and `INSERT ... ON DUPLICATE KEY UPDATE`, storing the player UUID as `BINARY(16)` and encoding the three `Selection` states
- [ ] 7.5 Implement startup connectivity validation with the `ABORT` and `FALLBACK_YAML` policies, logging the decision taken, and abort startup on an unrecognized backend value naming the accepted values
- [ ] 7.6 Implement runtime failure handling: report the configured storage-failure message to the acting player, log the cause, keep in-memory state consistent with what persisted, and degrade reads to the last cached value
- [ ] 7.7 Verify connection loss and restoration during play resumes writes without a restart while cached reads keep serving players
- [ ] 7.8 Verify the per-join selection lookup is a single indexed access returning at most one row per kind, and that a second startup against the existing schema issues no destructive statement

## 8. Chat, nametag and tab rendering — branch `feature/nameplate-rendering` → `develop`

- [ ] 8.1 Define the `CosmeticRenderer` port and a composite that dispatches to only the enabled targets, registering no listener or task for a disabled target
- [ ] 8.2 Implement `ChatRenderAdapter` on `AsyncChatEvent` with a `ChatRenderer`, using the configurable format, resolving the empty-tag case with no leftover separator, and inserting the message body as a literal component unless the sender holds the formatting permission
- [ ] 8.3 Implement `NametagRenderAdapter` using one scoreboard team per tag with the weight-encoded, length-bounded team name from `design.md` D7, touching only teams under our own prefix
- [ ] 8.4 Unregister every team the plugin created on disable and on the nametag target being turned off, and verify no residual prefix remains on any player
- [ ] 8.5 Implement `TabListRenderAdapter` setting the player list name and applying weight-based list ordering when the ordering mode is `weight`, leaving server default otherwise
- [ ] 8.6 Wire rendering to selection-change, join and quit events with no repeating task, and make the optional reconciliation pass configurable and disabled by default
- [ ] 8.7 Verify a selection change propagates to chat, nametag and tab for all online viewers without a reconnect, and that a joining player immediately sees everyone's current cosmetics
- [ ] 8.8 Verify each target can be disabled independently and that disabling all four leaves selection and persistence fully functional

## 9. Title hologram — branch `feature/title-hologram` → `develop`

- [ ] 9.1 Implement `TitleHologramRenderAdapter` spawning a `TextDisplay` as a passenger of the player, non-collidable, non-targetable, invulnerable, gravity-free, with the vertical offset applied through the transformation translation
- [ ] 9.2 Stamp each hologram with a persistent-data key identifying it as plugin-owned plus the owner UUID
- [ ] 9.3 Implement per-viewer visibility with `showEntity` / `hideEntity`, honoring the self-visibility option and hiding the hologram from any viewer the owning player is hidden from
- [ ] 9.4 Remove the hologram on title clear, on quit and on plugin disable, leaving no orphaned entity
- [ ] 9.5 Implement orphan cleanup at startup and on chunk load for holograms carrying our persistent-data key without a matching online owner, and verify an unclean shutdown leaves nothing behind after restart
- [ ] 9.6 Re-apply the offset to live holograms on config reload without a restart
- [ ] 9.7 Verify the hologram tracks the player through movement, teleport and world change with no visible detachment, and that titles remain independent of tags in both directions

## 10. Selector — branch `feature/cosmetic-selector` → `develop`

- [ ] 10.1 Define the `Selector` port and mode resolution, falling back to `MENU` on an unrecognized value with a warning naming the invalid value
- [ ] 10.2 Implement the shared selector view model: all entries of the requested kind, owned/locked marking, the hide-locked option, descending weight then ascending identifier ordering, active-entry marking, and the empty-selector message
- [ ] 10.3 Implement `MenuLayout` parsed from config (size, content slots, entry material, locked material, filler, navigation slots, title), logging each invalid value at startup and substituting the documented default
- [ ] 10.4 Implement `MenuSelector` with an `InventoryHolder` marker for click routing, pagination, and navigation controls shown only for directions that exist
- [ ] 10.5 Implement `ChatSelector` using Adventure `ClickEvent.callback` with an expiry, hover previews of the rendered prefix, non-clickable locked entries showing the required permission when configured, and an expired-click message
- [ ] 10.6 Implement the clear action, shown only when an active cosmetic exists unless configured to always show
- [ ] 10.7 Implement per-player open and select cooldowns checked before any use case runs, so a rate-limited request never reaches storage
- [ ] 10.8 Handle the entry-deleted-while-open case by refusing with the unknown-entry message and refreshing the view to the current catalogue
- [ ] 10.9 Verify the selector opens from cache with the database unreachable, and that attempting to change a selection then reports the storage-failure message

## 11. In-game authoring — branch `feature/in-game-authoring` → `develop`

- [ ] 11.1 Implement `AuthoringSession` as an immutable record and `AuthoringStep` as a sealed progression, where each input returns a new session
- [ ] 11.2 Implement `AuthoringSessionStore` over a `ConcurrentHashMap<UUID, AuthoringSession>` with a scheduled timeout sweep and removal on quit
- [ ] 11.3 Capture input by intercepting and cancelling `AsyncChatEvent` for players holding an active session, so flow input never reaches public chat
- [ ] 11.4 Implement the creation flow collecting identifier, prefix, permission and weight, each step stating what it asks and showing the current value, with a cancel input that aborts without persisting
- [ ] 11.5 Implement step-level validation through `CatalogueRules` that reports the specific problem and re-asks the same step without aborting, covering duplicate identifier, unparseable prefix and non-numeric weight
- [ ] 11.6 Implement the permission skip input defaulting to the configured pattern for that kind and identifier, and show the defaulted value in the confirmation summary
- [ ] 11.7 Implement the confirmation summary with an operator-only rendered preview across every enabled render target, including a viewer-scoped title preview that leaves no entity behind
- [ ] 11.8 Implement the edit flow for prefix, permission and weight one field at a time, refusing identifier edits with an immutability message, and refusing a commit whose target was deleted mid-flow
- [ ] 11.9 Implement deletion with an explicit confirmation naming the target and the count of online players currently using it, applying the per-kind fallback rule to affected players
- [ ] 11.10 Enforce identifier uniqueness at commit time on the storage thread, and verify two operators racing on one identifier results in exactly one success and one duplicate-identifier refusal
- [ ] 11.11 Verify a storage write failure shows the storage-failure message and leaves the cosmetic absent from the in-memory catalogue, and that an edited prefix re-renders for online players without a reconnect
- [ ] 11.12 Verify session discard on disconnect and on the configured timeout, in both cases persisting nothing

## 12. Commands and permissions — branch `feature/command-surface` → `develop`

- [ ] 12.1 Register commands through `LifecycleEvents.COMMANDS` using the Paper Brigadier `Commands` API, with per-node permission gating
- [ ] 12.2 Implement the player selector command for tags and titles, refusing console execution with the configured player-only message
- [ ] 12.3 Implement the operator command tree: list, create, edit, delete, force a player's selection, and reload
- [ ] 12.4 Implement the force action, refusing an unowned cosmetic unless the override flag is enabled, applying and logging it when enabled, and notifying the target player
- [ ] 12.5 Define every permission node in `paper-plugin.yml` with no operator action defaulting to all players, and document the full table in `README.md`
- [ ] 12.6 Implement tab completion for cosmetic kinds, existing identifiers and online player names, filtered to what the sender has permission to use
- [ ] 12.7 Return the configured usage message on invalid usage rather than a raw error, and verify no command path leaks a stack trace to the sender

## 13. PlaceholderAPI integration — branch `feature/placeholderapi` → `develop`

- [ ] 13.1 Define the `PlaceholderResolver` port with a no-op implementation, and confine every PlaceholderAPI type reference to `infrastructure.placeholder`
- [ ] 13.2 Implement `StagsExpansion` publishing active tag id, tag prefix, tag weight, active title id, title text, owned tag count and owned title count, resolving to the configured empty value when absent
- [ ] 13.3 Resolve an unrecognized placeholder in our namespace to nothing rather than an error string, and resolve any published placeholder for an offline player to the empty value with no storage query on the calling thread
- [ ] 13.4 Detect PlaceholderAPI at enable and on `PluginEnableEvent` so a later-loading installation still activates the integration, logging when it becomes active
- [ ] 13.5 Resolve placeholders inside cosmetic prefixes for the wearing player in a single non-recursive pass, using the placeholder-bearing flag computed at load/edit time so a static prefix bypasses resolution entirely
- [ ] 13.6 Verify the plugin enables with no error and no per-render logging when PlaceholderAPI is absent, and renders placeholder text literally
- [ ] 13.7 Verify disabling PlaceholderAPI at runtime makes the plugin render placeholders literally without erroring or self-disabling

## 14. Hardening and verification — branch `feature/hardening` → `develop`

- [ ] 14.1 Audit every source file for compliance with the project conventions: no comments in `.java` or `.kts`, English-only identifiers and strings, `final` fields, no setters, no static singletons
- [ ] 14.2 Verify the dependency direction holds — `domain` has no Bukkit, Paper or JDBC import, and `application` references only `domain` and its own ports
- [ ] 14.3 Run a soak test with both backends: join, select, clear, author, edit, delete, reload and quit, confirming no main-thread storage access and no tick lag attributable to the plugin
- [ ] 14.4 Verify a shutdown/startup cycle leaves no orphaned hologram, no residual scoreboard team and no unflushed write, under both a clean stop and a killed process
- [ ] 14.5 Verify every scenario in the eight spec files against a live Paper 26.2 server and record the result for each
- [ ] 14.6 Confirm `./gradlew build` passes from a clean checkout on JDK 25 with no warnings introduced by this change

## 15. Release 0.1.0 — branch `release/0.1.0` → `main` and `develop`

- [ ] 15.1 Cut `release/0.1.0` from `develop` and confirm the project version reads `0.1.0` in `build.gradle.kts` and in the built `paper-plugin.yml`
- [ ] 15.2 Finalize `CHANGELOG.md` for 0.1.0, moving the `Unreleased` entries into a dated release section
- [ ] 15.3 Complete `README.md`: requirements, installation, both backends with example configuration, the command and permission table, the configuration reference, and the PlaceholderAPI placeholder list
- [ ] 15.4 Verify the released jar on a clean Paper 26.2 server with an empty data folder and no database, confirming a working default install out of the box
- [ ] 15.5 Merge `release/0.1.0` into `main`, tag `v0.1.0`, and confirm the release workflow attaches the shaded jar
- [ ] 15.6 Merge `release/0.1.0` back into `develop` and delete the release branch
