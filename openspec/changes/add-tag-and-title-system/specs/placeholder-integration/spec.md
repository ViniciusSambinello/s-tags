## Purpose

Defines the placeholders this plugin publishes for other plugins to consume, and how placeholders written by an operator inside a cosmetic prefix are resolved, when PlaceholderAPI is installed.

## ADDED Requirements

### Requirement: Optional dependency

PlaceholderAPI SHALL be an optional dependency. The plugin SHALL function completely without it, and SHALL detect its presence at startup and after it is enabled later in the startup order.

#### Scenario: PlaceholderAPI absent

- **WHEN** the plugin starts on a server without PlaceholderAPI
- **THEN** the plugin enables normally with every feature except placeholder resolution working
- **AND** no error is logged

#### Scenario: PlaceholderAPI present

- **WHEN** the plugin starts on a server with PlaceholderAPI
- **THEN** the plugin registers its expansion
- **AND** the log records that the integration is active

#### Scenario: PlaceholderAPI disabled at runtime

- **WHEN** PlaceholderAPI is disabled while the server is running
- **THEN** the plugin stops resolving placeholders and renders them literally
- **AND** the plugin does not error or disable itself

### Requirement: Published placeholders

When the integration is active the plugin SHALL publish, at minimum, placeholders resolving to: the player's active tag identifier, the active tag's rendered prefix, the active tag's weight, the active title identifier, the active title's rendered text, the number of tags the player owns, and the number of titles the player owns. A placeholder for a player with no active cosmetic SHALL resolve to the configured empty value rather than to a literal null or an error string.

#### Scenario: Player with an active tag

- **WHEN** another plugin resolves the active-tag-prefix placeholder for a player whose active tag is `vip`
- **THEN** the `vip` prefix is returned as rendered text

#### Scenario: Player with no active tag

- **WHEN** the active-tag-prefix placeholder is resolved for a player with no active tag
- **THEN** the configured empty value is returned

#### Scenario: Unknown placeholder requested

- **WHEN** an unrecognized placeholder in this plugin's namespace is requested
- **THEN** the request resolves to nothing rather than to an error string

#### Scenario: Placeholder resolved for an offline player

- **WHEN** a published placeholder is resolved for a player who is not online
- **THEN** the configured empty value is returned without a storage query on the calling thread

### Requirement: Placeholder resolution inside cosmetic prefixes

When the integration is active, placeholders written inside a tag or title prefix SHALL be resolved against the wearing player before the prefix is rendered. When the integration is inactive the placeholder text SHALL be rendered literally without error.

#### Scenario: Prefix containing a placeholder

- **WHEN** a tag prefix contains a third-party placeholder and PlaceholderAPI is present
- **THEN** the placeholder is resolved for the wearing player before rendering

#### Scenario: Prefix containing a placeholder without PlaceholderAPI

- **WHEN** a tag prefix contains a placeholder and PlaceholderAPI is absent
- **THEN** the placeholder text is rendered literally
- **AND** no error is logged per render

### Requirement: Resolution safety and cost

Placeholder resolution inside prefixes SHALL be bounded: resolution SHALL NOT recurse into its own output, and a prefix containing no placeholder SHALL bypass resolution entirely. Prefixes containing placeholders SHALL be identified once when the cosmetic is loaded or edited, not re-scanned on every render.

#### Scenario: Static prefix

- **WHEN** a prefix contains no placeholder
- **THEN** rendering performs no placeholder resolution

#### Scenario: Self-referential placeholder

- **WHEN** a tag prefix contains this plugin's own active-tag-prefix placeholder
- **THEN** resolution does not recurse and the result is bounded to a single pass
