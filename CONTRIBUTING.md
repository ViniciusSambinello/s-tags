# Contributing to s-tags

## Branching model

This project follows [GitFlow](https://nvie.com/posts/a-successful-git-branching-model/).

- `main` holds tagged releases only. Every commit on `main` is a release.
- `develop` is the integration branch. All finished work lands here first.
- `feature/<name>` branches off `develop` for new capabilities.
- `release/<version>` branches off `develop` to stabilize a release candidate,
  then merges into both `main` and `develop`.
- `hotfix/<name>` branches off `main` for urgent production fixes, then merges
  into both `main` and `develop`.

Open a pull request into `develop` (or `main` for a hotfix) for every change.
Direct pushes to `main` and `develop` are not permitted.

## Commit messages

This project uses [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<optional scope>): <description>

[optional body]

[optional footer(s)]
```

Common types: `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `build`, `ci`, `chore`.

## Code conventions

- **Language:** every identifier, log message, player-facing string, commit
  message and comment is written in English.
- **No code comments.** Source files (`.java`, `.kts`) never contain comments.
  Code must be self-explanatory through naming and structure. Comments are
  permitted only in `.yml` resource files, to help a server operator
  understand a configuration key.
- **Immutability.** Classes are `final`, fields are `final`, value types are
  `record`s, closed hierarchies are `sealed` interfaces, collections are
  copied defensively (`List.copyOf`, `Map.copyOf`). Mutation is confined to
  explicitly owned concurrent state holders (for example a cache keyed by
  player UUID); nothing else is mutated in place.
- **Clean architecture / SOLID.** The codebase is layered as `domain` →
  `application` → `infrastructure`, with dependencies pointing inward only.
  `domain` never imports Bukkit, Paper or JDBC types. Collaborators are wired
  through constructors; there are no static singletons and no service
  locators.
- **Performance.** No storage I/O (JDBC or filesystem) runs on the server's
  main thread. The cosmetic catalogue is loaded once at startup and cached in
  memory; storage is written through on mutation only.
- **Configuration.** Anything player-facing or operationally relevant belongs
  in `config.yml` or `messages.yml`, not hard-coded.

## Requirements

- JDK 25
- Paper 26.2 for running and manual verification
- `./gradlew build` must pass before opening a pull request
