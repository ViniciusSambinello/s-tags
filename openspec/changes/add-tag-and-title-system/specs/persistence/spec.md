## Purpose

Defines the storage contract for cosmetic definitions and player selections, the two interchangeable backends a server operator can choose between, and the caching, concurrency and failure behavior that applies to both.

## ADDED Requirements

### Requirement: Interchangeable storage backends

The system SHALL support exactly two storage backends — `MYSQL` and `YAML` — selected by a single configuration value. Both SHALL implement identical externally observable behavior for storing and retrieving cosmetic definitions and player selections. An unrecognized backend value SHALL abort plugin startup with a log entry naming the invalid value and the accepted values.

#### Scenario: YAML backend selected

- **WHEN** the storage backend is set to `YAML`
- **THEN** cosmetic definitions and player selections are stored in the plugin's data folder
- **AND** no database connection is attempted

#### Scenario: MySQL backend selected

- **WHEN** the storage backend is set to `MYSQL` with valid credentials
- **THEN** the plugin connects at startup, ensures its schema exists, and stores all data in the database

#### Scenario: Unknown backend selected

- **WHEN** the storage backend is set to an unrecognized value
- **THEN** the plugin does not enable
- **AND** the log states the invalid value and lists the accepted values

### Requirement: Startup connectivity validation

When the MySQL backend is selected, the system SHALL validate connectivity and schema availability during startup, before any player can join. If the database is unreachable, the plugin SHALL either abort startup or fall back to the YAML backend according to the configured failure policy, and SHALL log the decision.

#### Scenario: Database unreachable with abort policy

- **WHEN** MySQL is selected, the failure policy is `ABORT`, and the database is unreachable at startup
- **THEN** the plugin disables itself with a log entry stating the connection failure
- **AND** no cosmetics are rendered

#### Scenario: Database unreachable with fallback policy

- **WHEN** MySQL is selected, the failure policy is `FALLBACK_YAML`, and the database is unreachable at startup
- **THEN** the plugin enables using the YAML backend
- **AND** the log states that the fallback was taken and why

### Requirement: No blocking storage access on the main thread

No storage read or write SHALL execute on the server's main thread. Every operation that reaches a database or the filesystem SHALL run on a dedicated worker, and results that affect gameplay state SHALL be applied back on the main thread.

#### Scenario: Player joins while the database is slow

- **WHEN** a player joins and the database responds slowly
- **THEN** the server's main thread is not blocked and no tick lag is attributable to the plugin
- **AND** the player's cosmetics apply once the load completes

#### Scenario: Bulk selection write

- **WHEN** many players change selections in quick succession
- **THEN** all writes execute off the main thread
- **AND** the server tick rate is unaffected

### Requirement: In-memory catalogue cache

The full cosmetic catalogue SHALL be loaded into memory once at startup and kept authoritative in memory thereafter, with storage written through on every mutation. Catalogue reads SHALL never reach storage after startup.

#### Scenario: Repeated catalogue reads

- **WHEN** a thousand catalogue reads occur after startup
- **THEN** no storage query is issued

#### Scenario: Catalogue mutated in-game

- **WHEN** a cosmetic is created, edited or deleted at runtime
- **THEN** the change is written to storage and the in-memory catalogue reflects it after the write succeeds

### Requirement: Per-player selection lifecycle

A player's selections SHALL be loaded when they join and released when they quit. While the player is online their selections SHALL be served from memory. A selection change SHALL update memory immediately and be written through to storage asynchronously.

#### Scenario: Selection loaded on join

- **WHEN** a player joins
- **THEN** their persisted tag and title selections are loaded exactly once
- **AND** their cosmetics are applied once loading completes

#### Scenario: Selection released on quit

- **WHEN** a player quits
- **THEN** any pending write for that player is flushed
- **AND** their cached selections are released from memory

#### Scenario: Player quits immediately after selecting

- **WHEN** a player changes a selection and disconnects before the write completes
- **THEN** the write still completes
- **AND** the selection is present when they rejoin

### Requirement: Selection storage semantics

A player's stored selection SHALL distinguish three states per cosmetic kind: no record (never chosen), an explicit cosmetic identifier, and an explicit cleared state. Writing a selection SHALL be an upsert keyed by player identity and cosmetic kind.

#### Scenario: Never chosen

- **WHEN** a player has no stored tag record and joins
- **THEN** the automatic default rule for tags is applied

#### Scenario: Explicitly cleared

- **WHEN** a player has an explicitly cleared tag record and joins
- **THEN** no tag is applied and the automatic default rule is not used

#### Scenario: Repeated selection of the same value

- **WHEN** a player selects the cosmetic that is already active
- **THEN** the state is unchanged and no redundant storage write is issued

### Requirement: MySQL schema and indexing

The MySQL backend SHALL create and own its tables at startup if they do not exist, using a configurable table prefix. Player selections SHALL be keyed by the player's unique identifier so that a lookup for one player is an indexed single-row access, and cosmetic definitions SHALL be keyed by kind and identifier.

#### Scenario: First startup on an empty database

- **WHEN** the plugin starts against a database with none of its tables
- **THEN** the required tables and indexes are created
- **AND** startup completes successfully

#### Scenario: Startup against an existing schema

- **WHEN** the plugin starts against a database that already holds its current schema
- **THEN** no destructive statement is issued and existing data is preserved

#### Scenario: Player selection lookup

- **WHEN** a single player's selections are loaded
- **THEN** the lookup uses the primary key on the player identifier and returns at most one row per cosmetic kind

### Requirement: Schema versioning and migration

The MySQL backend SHALL record its schema version and SHALL apply forward migrations in order on startup when the stored version is older than the plugin's version. A stored version newer than the plugin's SHALL abort startup rather than risk operating against an unknown schema.

#### Scenario: Fresh install records the version

- **WHEN** the schema is created for the first time
- **THEN** the current schema version is recorded

#### Scenario: Newer schema than the plugin supports

- **WHEN** the stored schema version is newer than the plugin's supported version
- **THEN** the plugin does not enable
- **AND** the log names both versions

### Requirement: YAML backend write batching

The YAML backend SHALL keep its state in memory and SHALL batch and debounce writes to disk on a configurable interval rather than rewriting the file on every mutation. Pending changes SHALL be flushed on plugin shutdown. Writes SHALL be atomic so an interrupted write cannot leave a truncated or unparseable file.

#### Scenario: Many rapid changes

- **WHEN** many selections change within one debounce interval
- **THEN** the file is written once for that interval, containing all changes

#### Scenario: Shutdown with pending changes

- **WHEN** the plugin shuts down with unflushed changes
- **THEN** the pending changes are written before shutdown completes

#### Scenario: Write interrupted

- **WHEN** the process is killed during a disk write
- **THEN** the previously valid file remains readable on next startup
- **AND** no partially written file replaces it

### Requirement: Storage failure behavior

A storage failure during a runtime operation SHALL be reported to the acting player with the configured message, SHALL be logged with the underlying cause, and SHALL NOT leave in-memory state inconsistent with what was successfully persisted. A read failure SHALL degrade to the last known cached value rather than to an exception reaching the server.

#### Scenario: Write fails at runtime

- **WHEN** a selection write fails
- **THEN** the acting player is shown the configured storage-failure message
- **AND** the failure with its cause is logged
- **AND** in-memory state matches what is persisted

#### Scenario: Connection is lost and restored

- **WHEN** the database connection is lost during play and later restored
- **THEN** the plugin resumes writing without a restart
- **AND** cached reads continue serving players throughout
