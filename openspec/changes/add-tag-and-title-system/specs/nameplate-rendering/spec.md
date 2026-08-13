## Purpose

Defines where and how a player's active cosmetics become visible to other players — the chat prefix, the nametag prefix above the head, the tab list entry, and the floating hologram line that carries the title.

## ADDED Requirements

### Requirement: Independently toggleable render targets

The system SHALL support four render targets — chat, nametag, tab list and title hologram — and each SHALL be independently enabled or disabled in configuration. Disabling a target SHALL leave that surface entirely untouched by the plugin, so it can coexist with another plugin that owns that surface.

#### Scenario: All targets enabled

- **WHEN** every render target is enabled and a player has an active tag and an active title
- **THEN** the tag prefix appears in the player's chat messages, on their nametag and in the tab list
- **AND** the title appears as a hologram above their nametag

#### Scenario: Chat target disabled

- **WHEN** the chat render target is disabled
- **THEN** the plugin does not modify any chat message
- **AND** the nametag, tab list and title hologram still reflect the player's cosmetics

#### Scenario: All targets disabled

- **WHEN** every render target is disabled
- **THEN** the plugin registers no rendering behavior against chat, scoreboard, tab list or entities
- **AND** selection and persistence still function

### Requirement: Chat rendering

When the chat target is enabled, the system SHALL render each chat message using a configurable format that supports at minimum the player's tag prefix, display name and message body. The message body SHALL NOT be interpreted as formatting markup unless the sender holds the configured formatting permission.

#### Scenario: Player with a tag chats

- **WHEN** a player with active tag `<gold>[VIP]</gold>` sends the message `hello`
- **THEN** other players see the configured format with the tag prefix resolved and the body `hello`

#### Scenario: Player without a tag chats

- **WHEN** a player with no active tag sends a message
- **THEN** the tag placeholder in the format resolves to the configured empty value with no leftover separator artifacts

#### Scenario: Message body contains markup

- **WHEN** a player without the formatting permission sends a message containing MiniMessage or legacy colour markup
- **THEN** the markup is rendered literally as typed and applies no formatting

#### Scenario: Player with formatting permission

- **WHEN** a player holding the configured formatting permission sends a message containing markup
- **THEN** the markup is resolved and the formatting is applied

### Requirement: Nametag rendering

When the nametag target is enabled, the system SHALL display the active tag prefix immediately before the player's name above their head. The plugin SHALL only manage entries it created and SHALL restore the player's untouched nametag state when the plugin is disabled or the target is turned off.

#### Scenario: Tag applied to nametag

- **WHEN** a player's active tag changes to `vip`
- **THEN** the `vip` prefix appears before their name above their head for all viewers

#### Scenario: Tag cleared from nametag

- **WHEN** a player clears their active tag
- **THEN** their name above their head renders with no prefix

#### Scenario: Plugin shuts down

- **WHEN** the plugin is disabled
- **THEN** every nametag grouping the plugin created is removed
- **AND** players are left with no residual prefix from this plugin

### Requirement: Tab list rendering

When the tab list target is enabled, the system SHALL display the active tag prefix before the player's name in the tab list. Tab list ordering SHALL follow the active tag's weight in descending order when the configured ordering mode is `weight`, and SHALL be left to the server default otherwise.

#### Scenario: Tab list shows prefix

- **WHEN** a player has active tag `vip`
- **THEN** their tab list entry shows the `vip` prefix before their name

#### Scenario: Weight ordering enabled

- **WHEN** tab ordering is set to `weight` and two players have tags of weight 100 and 10
- **THEN** the player with weight 100 is listed above the player with weight 10

### Requirement: Title hologram rendering

When the title hologram target is enabled, a player with an active title SHALL have that title rendered as a floating line positioned above their nametag, following the player's position and rotation. The hologram SHALL be non-collidable, non-targetable, invulnerable, unaffected by gravity, and SHALL NOT be visible to the player who owns it unless the configured self-visibility option is enabled.

#### Scenario: Title becomes visible to others

- **WHEN** a player selects the `champion` title
- **THEN** every other player in render distance sees `Champion` floating above that player's nametag

#### Scenario: Owner self-visibility disabled

- **WHEN** self-visibility is disabled and a player has an active title
- **THEN** that player does not see their own title hologram
- **AND** all other players do

#### Scenario: Hologram follows the player

- **WHEN** a player with an active title moves, teleports or changes worlds
- **THEN** the hologram remains positioned above their nametag without a visible detachment

#### Scenario: Title cleared

- **WHEN** a player clears their active title
- **THEN** the hologram is removed from every viewer

#### Scenario: Player leaves

- **WHEN** a player with an active title disconnects
- **THEN** their hologram is removed and leaves no orphaned entity behind

#### Scenario: Player becomes hidden or invisible

- **WHEN** a player with an active title is hidden from a specific viewer by the server or another plugin
- **THEN** that viewer does not see the player's title hologram

### Requirement: Orphaned hologram cleanup

The system SHALL remove hologram entities it previously created but no longer owns — for example after an unclean server shutdown — on plugin startup and on chunk load, so holograms never accumulate across restarts.

#### Scenario: Server restarts uncleanly

- **WHEN** the server is killed while title holograms exist and is then restarted
- **THEN** the previously created hologram entities are removed during startup or when their chunk loads
- **AND** a fresh hologram is created for each online player with an active title

### Requirement: Render refresh

Cosmetic changes SHALL propagate to every enabled render target without requiring the affected player or any viewer to reconnect. Rendering SHALL be driven by change events rather than by continuous polling; any periodic reconciliation pass SHALL be configurable and disabled by default.

#### Scenario: Selection change propagates

- **WHEN** a player changes their active tag
- **THEN** chat, nametag and tab list reflect the new tag for all online viewers without a reconnect

#### Scenario: Viewer joins after a change

- **WHEN** a player joins the server after another player changed cosmetics
- **THEN** the joining player immediately sees the current cosmetics of everyone online
