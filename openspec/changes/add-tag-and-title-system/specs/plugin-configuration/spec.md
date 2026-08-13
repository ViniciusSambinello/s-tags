## Purpose

Defines the externally observable configuration surface of the plugin — what a server operator can change, how invalid values behave, how player-facing messages are formatted, and how configuration is reloaded at runtime.

## ADDED Requirements

### Requirement: Configuration files and defaults

The plugin SHALL ship a `config.yml` for behavior and a `messages.yml` for every player-facing string, both written to the plugin data folder on first run. Every key SHALL have a documented default, and the shipped files SHALL carry explanatory comments for each setting. A key missing from an operator's file SHALL resolve to its default without failing startup.

#### Scenario: First run

- **WHEN** the plugin starts with an empty data folder
- **THEN** `config.yml` and `messages.yml` are created with defaults and explanatory comments

#### Scenario: Missing key

- **WHEN** an operator's `config.yml` omits a key that the plugin expects
- **THEN** the documented default is used
- **AND** startup succeeds

#### Scenario: Unknown key present

- **WHEN** an operator's `config.yml` contains a key the plugin does not recognize
- **THEN** the key is ignored and startup succeeds
- **AND** the unrecognized key is reported once in the log

### Requirement: Configurable behavior surface

Configuration SHALL expose, at minimum: the storage backend and its connection settings, the database failure policy, the YAML write debounce interval, the selector mode, the selector menu layout, whether locked entries are shown, selector cooldowns, each render target's enabled flag, the chat format, the tab list ordering mode, title hologram offset and self-visibility, the authoring flow timeout, the default permission pattern for authored cosmetics, and the reconciliation interval.

#### Scenario: Operator disables a render target

- **WHEN** an operator sets the tab list render target to disabled and reloads
- **THEN** the plugin stops modifying tab list entries
- **AND** no other target is affected

#### Scenario: Operator changes the hologram offset

- **WHEN** an operator changes the title hologram vertical offset and reloads
- **THEN** existing holograms reposition to the new offset without a restart

### Requirement: Configuration validation

Every configuration value SHALL be validated at load. An invalid value SHALL be reported in the log with its key, the offending value and the accepted range or format, and SHALL fall back to its documented default — except for values that make correct operation impossible, which SHALL abort startup as specified by the capability that owns them.

#### Scenario: Out-of-range numeric value

- **WHEN** a cooldown is configured as a negative number
- **THEN** the log names the key, the value and the accepted range
- **AND** the documented default is used

#### Scenario: Wrong type

- **WHEN** a boolean setting is configured with a non-boolean value
- **THEN** the log names the key and the expected type
- **AND** the documented default is used

### Requirement: Message formatting

Every player-facing string SHALL be defined in `messages.yml`, SHALL be authored in MiniMessage, and SHALL support a documented set of placeholders scoped to that message. A message set to an empty string SHALL suppress that output entirely rather than sending a blank line.

#### Scenario: Operator customizes a message

- **WHEN** an operator changes the selection-confirmation message and reloads
- **THEN** players see the new text with placeholders resolved

#### Scenario: Message emptied

- **WHEN** an operator sets a message to an empty string
- **THEN** that message is not sent at all

#### Scenario: Unknown placeholder used

- **WHEN** an operator uses a placeholder that is not defined for that message
- **THEN** the placeholder is left literal in the output
- **AND** the plugin does not error

#### Scenario: Message fails to parse

- **WHEN** a configured message is not valid MiniMessage
- **THEN** the log names the message key and the parse problem
- **AND** the shipped default for that key is used

### Requirement: Configurable message prefix

A single configurable prefix SHALL be prepended to plugin messages, and each message SHALL be able to opt out of the prefix.

#### Scenario: Prefix applied

- **WHEN** a prefix is configured and a message that uses the prefix is sent
- **THEN** the message is rendered with the prefix

#### Scenario: Prefix opted out

- **WHEN** a message is marked to skip the prefix
- **THEN** it is rendered without the prefix

### Requirement: Runtime reload

An operator with the reload permission SHALL be able to reload `config.yml` and `messages.yml` at runtime. A reload SHALL re-validate every value, re-apply rendering to all online players, and SHALL NOT drop player selections, disconnect players or reopen the storage backend when the storage settings are unchanged.

#### Scenario: Successful reload

- **WHEN** an operator reloads after editing `messages.yml`
- **THEN** the new messages take effect immediately
- **AND** no player is disconnected and no selection is lost

#### Scenario: Reload with an invalid file

- **WHEN** an operator reloads while a configuration file is not parseable
- **THEN** the reload is refused with the parse error reported to the operator and the log
- **AND** the previously loaded configuration remains active

#### Scenario: Storage settings changed

- **WHEN** an operator changes the storage backend and reloads
- **THEN** the operator is told that a storage backend change requires a server restart
- **AND** the running backend is unchanged

### Requirement: Command and permission surface

The plugin SHALL expose a player command for opening the tag and title selectors and an operator command covering listing, authoring, editing, deleting, forcing a player's selection and reloading. Every operator action SHALL be gated behind its own permission node, and no operator action SHALL default to being granted to all players.

#### Scenario: Player without an operator permission

- **WHEN** a player without the authoring permission attempts to create a tag
- **THEN** the command is refused with the configured no-permission message
- **AND** no flow is started

#### Scenario: Operator forces a player's selection

- **WHEN** an operator with the force permission sets another player's active tag to one that player owns
- **THEN** the target player's active tag changes and is persisted
- **AND** the target is notified with the configured message

#### Scenario: Operator forces an unowned cosmetic

- **WHEN** an operator forces a cosmetic the target player does not own
- **THEN** the action is refused unless the configured override flag is enabled
- **AND** when the override is enabled the change applies and is logged

#### Scenario: Console executes an operator command

- **WHEN** the console executes an operator command that does not require a player context
- **THEN** the command succeeds and output is written to the console

#### Scenario: Console executes a player-only command

- **WHEN** the console executes the selector command
- **THEN** the command is refused with the configured player-only message

### Requirement: Command completion and feedback

Operator commands SHALL provide tab completion for cosmetic kinds, existing identifiers and online player names, filtered to what the sender has permission to use. Invalid usage SHALL respond with the configured usage message rather than a raw error.

#### Scenario: Tab completing an identifier

- **WHEN** an operator tab-completes the identifier argument of the delete command
- **THEN** existing identifiers of the selected cosmetic kind are suggested

#### Scenario: Invalid usage

- **WHEN** an operator runs a command with a missing required argument
- **THEN** the configured usage message for that command is shown
