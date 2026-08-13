## Purpose

Defines the interactive, step-by-step flow that lets a server operator create, edit, preview and delete tags and titles entirely in-game, without editing files or restarting the server.

## ADDED Requirements

### Requirement: Guided creation flow

An operator with the creation permission SHALL be able to start a guided flow that collects, one step at a time, the new cosmetic's identifier, display prefix, permission node and weight. Each step SHALL state what is being asked, show the current value when one exists, and accept a cancel input that aborts the flow without persisting anything.

#### Scenario: Operator completes the flow

- **WHEN** an operator starts the tag creation flow and supplies a valid identifier, prefix, permission and weight
- **THEN** a confirmation summary of all four values is shown
- **AND** on confirmation the tag is created, persisted and immediately available for selection

#### Scenario: Operator cancels mid-flow

- **WHEN** an operator supplies the cancel input at any step
- **THEN** the flow ends
- **AND** no cosmetic is created or modified

#### Scenario: Operator disconnects mid-flow

- **WHEN** an operator disconnects while a creation flow is active
- **THEN** the flow is discarded
- **AND** no cosmetic is created

#### Scenario: Flow times out

- **WHEN** an operator supplies no input for the configured flow timeout
- **THEN** the flow ends with the configured timeout message
- **AND** no cosmetic is created

### Requirement: Step-level validation with retry

Each step SHALL validate its input immediately against the same rules that govern a stored cosmetic. Invalid input SHALL report the specific problem and re-ask the same step rather than aborting the flow.

#### Scenario: Duplicate identifier supplied

- **WHEN** an operator supplies an identifier that already exists
- **THEN** the flow reports the conflict and asks for the identifier again
- **AND** the flow remains active

#### Scenario: Unparseable prefix supplied

- **WHEN** an operator supplies a prefix that is not valid MiniMessage
- **THEN** the flow reports the parse problem and asks for the prefix again

#### Scenario: Non-numeric weight supplied

- **WHEN** an operator supplies a weight that is not an integer
- **THEN** the flow reports the expected format and asks for the weight again

#### Scenario: Permission defaulted

- **WHEN** an operator supplies the skip input at the permission step
- **THEN** the permission defaults to the configured pattern for that cosmetic kind and identifier
- **AND** the defaulted value is shown in the confirmation summary

### Requirement: Live preview before commit

Before committing, the flow SHALL show the operator a rendered preview of the cosmetic as it will appear in each enabled render target. The preview SHALL be visible only to the operator and SHALL create no persistent state.

#### Scenario: Preview shown at confirmation

- **WHEN** an operator reaches the confirmation step of a tag flow
- **THEN** the rendered prefix is shown as it would appear in chat, on the nametag and in the tab list

#### Scenario: Title preview shown

- **WHEN** an operator reaches the confirmation step of a title flow
- **THEN** a preview of the hologram line is shown to that operator only
- **AND** no hologram entity is left behind when the flow ends

### Requirement: Interactive editing

An operator with the edit permission SHALL be able to edit an existing cosmetic's prefix, permission or weight through the same guided flow, targeting one field at a time. The identifier SHALL NOT be editable; changing it requires deleting and recreating the entry.

#### Scenario: Prefix edited

- **WHEN** an operator edits the prefix of an existing tag and confirms
- **THEN** the stored prefix is updated
- **AND** every online player with that tag active renders the new prefix without reconnecting

#### Scenario: Identifier edit attempted

- **WHEN** an operator attempts to edit a cosmetic's identifier
- **THEN** the attempt is refused with a message explaining that the identifier is immutable

#### Scenario: Edit target no longer exists

- **WHEN** an operator confirms an edit for a cosmetic that was deleted while the flow was active
- **THEN** the edit is refused with the configured "unknown entry" message
- **AND** nothing is persisted

### Requirement: Deletion with confirmation

An operator with the delete permission SHALL be able to delete a cosmetic, and deletion SHALL require an explicit confirmation step that names the target and states how many online players currently have it active.

#### Scenario: Deletion confirmed

- **WHEN** an operator confirms deletion of the `vip` tag
- **THEN** the tag is removed from the catalogue and from storage
- **AND** every player who had it active falls back according to the fallback rule for that cosmetic kind

#### Scenario: Deletion not confirmed

- **WHEN** an operator starts a deletion and does not confirm
- **THEN** the cosmetic remains unchanged

### Requirement: Authoring persists to the active backend

Every cosmetic created, edited or deleted in-game SHALL be written to whichever storage backend is active and SHALL survive a server restart. The write SHALL NOT block the server's main thread, and the in-memory catalogue SHALL only be updated after the write succeeds.

#### Scenario: Created tag survives a restart

- **WHEN** an operator creates a tag in-game and the server is restarted
- **THEN** the tag is present after startup with the same prefix, permission and weight

#### Scenario: Storage write fails

- **WHEN** the storage write for a created cosmetic fails
- **THEN** the operator is shown the configured storage-failure message
- **AND** the cosmetic does not appear in the in-memory catalogue

### Requirement: Concurrent authoring safety

Two operators SHALL NOT be able to create conflicting cosmetics simultaneously. Identifier uniqueness SHALL be enforced at commit time, not only at the input step.

#### Scenario: Two operators race on one identifier

- **WHEN** two operators each complete a creation flow for the identifier `vip` and confirm at the same time
- **THEN** exactly one creation succeeds
- **AND** the other is refused with the duplicate-identifier message
