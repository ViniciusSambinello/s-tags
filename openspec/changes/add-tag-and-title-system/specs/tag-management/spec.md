## Purpose

Defines what a tag is, how a tag is validated, how a player comes to own tags, and how a player's single active tag is chosen, changed, cleared and resolved when ownership changes.

## ADDED Requirements

### Requirement: Tag definition

A tag SHALL be an operator-defined cosmetic entry composed of a unique identifier, a display prefix, a permission node, a weight and optional selector metadata. Identifiers SHALL be case-insensitive, unique across all tags, and restricted to lowercase letters, digits, hyphen and underscore, with a length between 1 and 32 characters. The display prefix SHALL be authored in MiniMessage format and SHALL be at most 128 characters before formatting is resolved. The weight SHALL be an integer used for ordering and for automatic fallback, where a higher weight ranks first.

#### Scenario: Valid tag is accepted

- **WHEN** a tag is defined with id `vip`, prefix `<gold>[VIP]</gold>`, permission `stags.tag.vip` and weight `100`
- **THEN** the tag is accepted and becomes available for ownership and selection

#### Scenario: Duplicate identifier is rejected

- **WHEN** a tag is defined with an identifier that differs only by letter case from an existing tag
- **THEN** the definition is rejected with an error naming the conflicting identifier
- **AND** the existing tag remains unchanged

#### Scenario: Malformed identifier is rejected

- **WHEN** a tag is defined with an identifier containing a space, a period or an uppercase-only character set outside the allowed alphabet
- **THEN** the definition is rejected with an error stating the allowed identifier format

#### Scenario: Malformed prefix is rejected

- **WHEN** a tag is defined with a prefix that is not parseable as MiniMessage
- **THEN** the definition is rejected with an error that identifies the offending portion of the prefix
- **AND** no partially-created tag is persisted

### Requirement: Permission-derived ownership

A player SHALL own a tag if and only if the player holds that tag's permission node. Ownership SHALL be evaluated against the player's live permission state and SHALL NOT be stored. A tag whose permission node is empty SHALL be owned by every player.

#### Scenario: Player holds the permission

- **WHEN** a player holds `stags.tag.vip` and the `vip` tag exists
- **THEN** the `vip` tag is reported as owned by that player

#### Scenario: Player does not hold the permission

- **WHEN** a player does not hold `stags.tag.vip`
- **THEN** the `vip` tag is reported as not owned by that player

#### Scenario: Permission is revoked at runtime

- **WHEN** a player's permission for their active tag is revoked while they are online
- **THEN** on the next ownership evaluation the tag is no longer reported as owned
- **AND** the active-tag fallback behavior is applied

### Requirement: Active tag selection

A player SHALL have at most one active tag at a time. Selecting a tag the player owns SHALL replace any previously active tag, SHALL be persisted, and SHALL take visual effect without requiring a reconnect.

#### Scenario: Player selects an owned tag

- **WHEN** a player who owns `vip` selects the `vip` tag
- **THEN** `vip` becomes the player's active tag
- **AND** the selection is persisted
- **AND** the player's rendered cosmetics update within the configured refresh boundary

#### Scenario: Player selects a tag they do not own

- **WHEN** a player who does not own `vip` attempts to select the `vip` tag
- **THEN** the selection is refused with the configured "not owned" message
- **AND** the player's previously active tag is unchanged

#### Scenario: Player selects an unknown tag

- **WHEN** a player attempts to select a tag identifier that does not exist
- **THEN** the selection is refused with the configured "unknown tag" message

### Requirement: Clearing the active tag

A player SHALL be able to clear their active tag, leaving them with no tag rendered. Clearing SHALL be persisted and SHALL be distinguishable from never having selected a tag only in that the automatic default is not re-applied.

#### Scenario: Player clears their tag

- **WHEN** a player with an active tag clears their selection
- **THEN** no tag is rendered for that player
- **AND** the cleared state is persisted
- **AND** rejoining the server does not restore the previous tag

### Requirement: Automatic default and fallback

When a player has no persisted selection, the system SHALL activate the highest-weight tag the player owns, breaking ties by identifier in ascending order. When a player's persisted active tag becomes unavailable — because the tag was deleted or the player lost its permission — the system SHALL fall back to the same rule and SHALL notify the player with the configured message if they are online.

#### Scenario: First join with owned tags

- **WHEN** a player with no persisted selection joins and owns `vip` (weight 100) and `member` (weight 10)
- **THEN** `vip` becomes their active tag

#### Scenario: First join with no owned tags

- **WHEN** a player with no persisted selection joins and owns no tags
- **THEN** no tag is rendered and no selection is persisted

#### Scenario: Active tag is deleted

- **WHEN** the `vip` tag is deleted while a player has it active
- **THEN** the player falls back to the highest-weight tag they still own, or to no tag if none remain
- **AND** the online player receives the configured fallback notification

#### Scenario: Tie on weight

- **WHEN** a player owns two tags of equal weight with identifiers `alpha` and `beta` and has no persisted selection
- **THEN** `alpha` becomes their active tag

### Requirement: Tag catalogue queries

The system SHALL expose the full tag catalogue and the subset a given player owns, both ordered by descending weight then ascending identifier. Catalogue reads SHALL be served without blocking the server's main thread on storage.

#### Scenario: Listing all tags

- **WHEN** an operator requests the tag list
- **THEN** every defined tag is returned in descending weight order
- **AND** the response includes each tag's identifier, permission, weight and rendered prefix preview

#### Scenario: Listing owned tags

- **WHEN** a player requests their available tags
- **THEN** only tags whose permission they hold are returned, in descending weight order
