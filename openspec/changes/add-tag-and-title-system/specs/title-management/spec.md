## Purpose

Defines the title cosmetic — a second, independently selectable label that floats above a player's nametag to advertise an achievement — including its definition, validation, ownership and active-selection rules.

## ADDED Requirements

### Requirement: Title definition

A title SHALL be an operator-defined cosmetic entry composed of a unique identifier, a display text, a permission node, a weight and optional selector metadata. The identifier rules, prefix format rules and weight semantics SHALL be identical to those of a tag. Title identifiers SHALL occupy a namespace separate from tag identifiers, so a tag and a title MAY share an identifier without conflict.

#### Scenario: Valid title is accepted

- **WHEN** a title is defined with id `champion`, text `<gradient:#FFD700:#FFA500>Champion</gradient>`, permission `stags.title.champion` and weight `500`
- **THEN** the title is accepted and becomes available for ownership and selection

#### Scenario: Identifier shared with a tag

- **WHEN** a title is defined with id `vip` and a tag with id `vip` already exists
- **THEN** the title is accepted
- **AND** both entries remain independently selectable

#### Scenario: Duplicate title identifier is rejected

- **WHEN** a title is defined with an identifier that differs only by letter case from an existing title
- **THEN** the definition is rejected with an error naming the conflicting identifier

#### Scenario: Malformed title text is rejected

- **WHEN** a title is defined with text that is not parseable as MiniMessage
- **THEN** the definition is rejected with an error that identifies the offending portion
- **AND** no partially-created title is persisted

### Requirement: Title ownership and selection are independent of tags

A player's owned titles SHALL be derived from title permission nodes only, and the active title SHALL be stored and changed independently of the active tag. Changing one SHALL NOT alter the other.

#### Scenario: Player owns a title but no tag

- **WHEN** a player holds `stags.title.champion` and holds no tag permission
- **THEN** the player owns the `champion` title and owns no tags
- **AND** the title renders while no tag renders

#### Scenario: Selecting a title leaves the tag untouched

- **WHEN** a player with active tag `vip` selects the `champion` title
- **THEN** the active title becomes `champion`
- **AND** the active tag remains `vip`

#### Scenario: Player selects a title they do not own

- **WHEN** a player who does not own `champion` attempts to select it
- **THEN** the selection is refused with the configured "not owned" message
- **AND** the previously active title is unchanged

### Requirement: Clearing the active title

A player SHALL be able to clear their active title, after which no title is rendered above their nametag. The cleared state SHALL be persisted and SHALL survive a reconnect.

#### Scenario: Player clears their title

- **WHEN** a player with an active title clears their selection
- **THEN** no title is rendered above their nametag
- **AND** rejoining the server does not restore the previous title

### Requirement: Title default and fallback

Unlike tags, titles SHALL NOT be auto-assigned on first join; a player with no persisted title selection SHALL render no title. When a persisted active title becomes unavailable — because it was deleted or its permission was revoked — the title SHALL be cleared rather than replaced, and the online player SHALL receive the configured notification.

#### Scenario: First join with owned titles

- **WHEN** a player with no persisted title selection joins and owns `champion`
- **THEN** no title is rendered
- **AND** the `champion` title remains available in their selector

#### Scenario: Active title permission is revoked

- **WHEN** a player's permission for their active title is revoked
- **THEN** the active title is cleared and no title is rendered
- **AND** the online player receives the configured fallback notification

### Requirement: Title catalogue queries

The system SHALL expose the full title catalogue and the subset a given player owns, both ordered by descending weight then ascending identifier, without blocking the server's main thread on storage.

#### Scenario: Listing owned titles

- **WHEN** a player requests their available titles
- **THEN** only titles whose permission they hold are returned, in descending weight order
