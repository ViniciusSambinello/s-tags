# s-tags

A [Paper](https://papermc.io/) 26.2 plugin providing two independent cosmetics
for players: **tags**, a permission-owned prefix rendered in chat, on the
nametag and in the tab list; and **titles**, a second cosmetic rendered as a
per-viewer hologram floating above the nametag, meant for things like
tournament achievements. Both are selected by the player, authored entirely
in-game by an operator, and persisted to either MySQL or a single YAML file.

## Requirements

- **Paper 26.2** (or a Paper-based fork built against the same API)
- **Java 25**
- Optionally [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)
  for `%stags_*%` placeholders
- Optionally a MySQL 8+ server, if the MySQL storage backend is used

## Installation

1. Download `s-tags-0.1.0.jar` from the [Releases](https://github.com/ViniciusSambinello/s-tags/releases)
   page.
2. Drop it into your server's `plugins/` folder.
3. Start the server once to generate `config.yml` and `messages.yml` under
   `plugins/s-tags/`.
4. By default the plugin stores data in `plugins/s-tags/cosmetics.yml` and
   `plugins/s-tags/selections.yml` — no database required. To use MySQL
   instead, see [Storage backends](#storage-backends) below.

## Storage backends

Selected with `storage.backend` in `config.yml`.

### YAML (default)

No configuration required. State is kept in memory and flushed to disk on a
debounced interval (`storage.yaml.write-interval-seconds`), so bursts of
changes cost one write, not one per change.

```yaml
storage:
  backend: YAML
  yaml:
    write-interval-seconds: 5
```

### MySQL

```yaml
storage:
  backend: MYSQL
  mysql:
    host: localhost
    port: 3306
    database: minecraft
    username: s_tags
    password: change-me
    table-prefix: stags_
    pool-size: 8
    failure-policy: ABORT # or FALLBACK_YAML
```

The plugin creates and owns three tables (`<prefix>schema_version`,
`<prefix>cosmetic`, `<prefix>player_selection`) on first startup and migrates
them forward automatically on later versions.

Switching the backend requires a server restart; 0.1.0 does not migrate data
between backends.

## Commands and permissions

| Command | Description | Permission |
|---|---|---|
| `/tag` | Open the tag selector | `stags.command.tag` |
| `/title` | Open the title selector | `stags.command.title` |
| `/stags create <tag\|title>` | Start the in-game authoring flow | `stags.admin.create` |
| `/stags edit <tag\|title> <id>` | Edit an existing cosmetic | `stags.admin.edit` |
| `/stags delete <tag\|title> <id>` | Delete a cosmetic | `stags.admin.delete` |
| `/stags list <tag\|title>` | List every defined cosmetic | `stags.admin.list` |
| `/stags force <player> <tag\|title> <id>` | Force a player's active selection | `stags.admin.force` |
| `/stags reload` | Reload `config.yml` and `messages.yml` | `stags.admin.reload` |

Owning a specific tag or title is controlled by that cosmetic's own
`permission` field (for example `stags.tag.vip`), configured per-entry when
the cosmetic is created. See [`design.md`](openspec/changes/add-tag-and-title-system/design.md)
for the full architecture and [`specs/`](openspec/changes/add-tag-and-title-system/specs)
for the complete behavior contract.

## Configuration

`config.yml` and `messages.yml` are fully documented with inline comments on
first generation. Every player-facing message, the selector mode (`MENU` or
`CHAT`), the menu layout, render targets, cooldowns and the hologram offset
are configurable without recompiling.

## PlaceholderAPI

When PlaceholderAPI is installed, `s-tags` publishes:

| Placeholder | Resolves to |
|---|---|
| `%stags_tag%` | The player's active tag identifier |
| `%stags_tag_prefix%` | The player's active tag, rendered |
| `%stags_tag_weight%` | The active tag's weight |
| `%stags_title%` | The player's active title identifier |
| `%stags_title_text%` | The player's active title, rendered |
| `%stags_tag_count%` | Number of tags the player owns |
| `%stags_title_count%` | Number of titles the player owns |

## Building from source

```bash
./gradlew build
```

Produces the shaded plugin jar at `build/libs/s-tags-0.1.0.jar`.

## Project structure

The codebase follows clean architecture: `domain` (pure Java, no Bukkit
dependency), `application` (use cases and ports), `infrastructure` (Bukkit,
JDBC and YAML adapters). See [`design.md`](openspec/changes/add-tag-and-title-system/design.md)
for the full rationale.

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for the branching model, commit
convention and code conventions.

## License

[MIT](LICENSE)
