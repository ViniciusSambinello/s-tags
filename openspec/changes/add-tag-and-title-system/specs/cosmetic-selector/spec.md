## Purpose

Defines the player-facing experience for browsing and choosing an active tag or title, available either as an inventory menu or as a chat-based list, with the mode chosen by the server operator.

## ADDED Requirements

### Requirement: Configurable selector mode

The selector SHALL operate in one of two modes — `MENU` or `CHAT` — chosen in configuration and applied to both the tag selector and the title selector. An unrecognized mode value SHALL fall back to `MENU` and SHALL be reported in the server log at startup.

#### Scenario: Menu mode configured

- **WHEN** the selector mode is `MENU` and a player opens the tag selector
- **THEN** an inventory menu of tags is displayed

#### Scenario: Chat mode configured

- **WHEN** the selector mode is `CHAT` and a player opens the tag selector
- **THEN** a chat list of tags is sent to the player

#### Scenario: Invalid mode configured

- **WHEN** the selector mode is set to an unrecognized value
- **THEN** the selector behaves as if `MENU` were configured
- **AND** a warning naming the invalid value is written to the server log

### Requirement: Selector contents

The selector SHALL list every defined cosmetic of the requested kind, marking each entry as owned or locked based on the viewing player's permissions, unless configuration requests that locked entries be hidden. Entries SHALL be ordered by descending weight then ascending identifier, and the player's currently active entry SHALL be visually distinguished.

#### Scenario: Locked entries shown

- **WHEN** locked entries are configured to be shown and a player opens the tag selector
- **THEN** tags the player does not own appear with the configured locked presentation

#### Scenario: Locked entries hidden

- **WHEN** locked entries are configured to be hidden
- **THEN** only tags the player owns appear in the selector

#### Scenario: Active entry indicated

- **WHEN** a player with active tag `vip` opens the tag selector
- **THEN** the `vip` entry is visually distinguished from the others

#### Scenario: Empty selector

- **WHEN** a player opens a selector that would contain no entries
- **THEN** the configured empty-selector message is shown instead of an empty interface

### Requirement: Selecting from the selector

Choosing an owned entry SHALL apply it as the player's active cosmetic, confirm with the configured message, and refresh the selector view or close it according to configuration. Choosing a locked entry SHALL refuse the change and show the configured locked message without altering the active cosmetic.

#### Scenario: Owned entry chosen

- **WHEN** a player chooses an owned entry in the selector
- **THEN** that entry becomes their active cosmetic
- **AND** the configured confirmation message is shown

#### Scenario: Locked entry chosen

- **WHEN** a player chooses a locked entry
- **THEN** the active cosmetic is unchanged
- **AND** the configured locked message is shown

#### Scenario: Entry deleted while the selector is open

- **WHEN** a player chooses an entry that was deleted after the selector was opened
- **THEN** the change is refused with the configured "unknown entry" message
- **AND** the selector view refreshes to the current catalogue

### Requirement: Clearing from the selector

The selector SHALL offer an action that clears the player's active cosmetic of that kind. The action SHALL be present only when the player currently has an active cosmetic, unless configuration requests it always be shown.

#### Scenario: Clear action used

- **WHEN** a player with an active tag uses the selector's clear action
- **THEN** their active tag is cleared
- **AND** the configured clear-confirmation message is shown

### Requirement: Menu mode layout and pagination

In `MENU` mode the inventory size, entry item material, locked-entry material, filler item, navigation slots and title SHALL be configurable. When entries exceed the available slots the menu SHALL paginate, and navigation controls SHALL appear only on pages where the corresponding direction exists.

#### Scenario: Entries fit one page

- **WHEN** the number of entries fits within the configured content slots
- **THEN** no navigation controls are displayed

#### Scenario: Entries span multiple pages

- **WHEN** the number of entries exceeds the configured content slots
- **THEN** the menu paginates
- **AND** the first page shows only a next control and the last page shows only a previous control

#### Scenario: Invalid layout configuration

- **WHEN** the configured inventory size is not a legal container size or a configured slot falls outside the inventory
- **THEN** the plugin logs the specific invalid value at startup and uses the documented default for that value

### Requirement: Chat mode presentation

In `CHAT` mode the selector SHALL send a configurable list in which each owned entry is clickable to select it and hoverable to preview it. Locked entries SHALL be non-clickable and SHALL show the required permission on hover when configuration permits.

#### Scenario: Clicking an owned entry in chat

- **WHEN** a player clicks an owned entry in the chat selector
- **THEN** that entry becomes their active cosmetic without them typing a command

#### Scenario: Hovering an entry in chat

- **WHEN** a player hovers an entry in the chat selector
- **THEN** a preview of the entry's rendered prefix is shown

### Requirement: Selector rate limiting

Opening a selector and choosing an entry SHALL each be subject to a configurable cooldown per player. Requests made inside the cooldown SHALL be rejected with the configured message and SHALL NOT reach storage.

#### Scenario: Rapid repeated selection

- **WHEN** a player changes their active cosmetic twice within the configured cooldown
- **THEN** the second change is rejected with the configured cooldown message
- **AND** no additional write reaches storage

### Requirement: Selector performance

Opening a selector SHALL NOT perform storage or disk I/O on the server's main thread. The catalogue and the player's active selection SHALL be served from memory.

#### Scenario: Selector opened with the database unreachable

- **WHEN** a player opens the selector while the configured database is unreachable
- **THEN** the selector still opens using cached data
- **AND** attempting to change a selection reports the configured storage-failure message
