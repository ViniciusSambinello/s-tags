# s-tags: Tag & Title Cosmetic System

**s-tags** is a permission-owned cosmetic system for Paper servers, giving each player two independently selectable cosmetics: **tags**, a prefix rendered in chat, on the nametag and in the tab list; and **titles**, a per-viewer hologram floating above the nametag, meant for things like tournament achievements.

## Core Capabilities

The system manages two independent cosmetic kinds—tags and titles—each defined by an operator with a MiniMessage-formatted prefix, an owning permission, and a weight controlling priority when a player owns several. Ownership is derived entirely from Bukkit permissions, so granting or revoking a cosmetic is exactly granting or revoking a permission node, with no separate entitlement store to keep in sync.

Titles render as a genuinely independent second cosmetic—a `TextDisplay` hologram riding as a passenger of the player—rather than a second slot in the tag system, so a server can run tournament titles and permission tags side by side with none of the selection or rendering logic entangled.

## Technical Requirements

The plugin requires Java 25 and Paper 26.2. PlaceholderAPI is optional, enabling `%stags_*%` placeholders and placeholder resolution inside cosmetic prefixes themselves. A MySQL 8+ server is optional as well—a single debounced YAML file is the default and requires no external database.

## Key Features

- **Dual cosmetic system** with independent tag and title selection, each with its own permission-owned catalogue and no shared default
- **Configurable selector**, an inventory menu or a chat-based list, switched with one config value and backed by per-action cooldowns
- **In-game interactive authoring** for creating, editing, previewing and deleting a tag or title entirely through chat, with no server restart or file editing required
- **Pluggable persistence**, MySQL via HikariCP with an auto-migrating schema, or a single debounced YAML file, with a configurable fallback policy if the database is unreachable
- **PlaceholderAPI soft integration** publishing player-facing placeholders and resolving placeholders inside cosmetic prefixes
- **Fully externalized configuration**, every message and render target reloadable at runtime via `config.yml`/`messages.yml`

## Implementation Path

Initial setup requires dropping the jar into `plugins/`, starting the server once to generate `config.yml` and `messages.yml`, and choosing a storage backend—YAML works immediately with no further configuration, while MySQL requires credentials and a reachable server, with the schema auto-creating and self-migrating on first connection. Cosmetics are then authored entirely in-game with `/stags create <tag|title>`, with no file editing or restart needed to add, edit or remove one.
