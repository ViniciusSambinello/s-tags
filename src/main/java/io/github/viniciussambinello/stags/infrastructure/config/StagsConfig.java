package io.github.viniciussambinello.stags.infrastructure.config;

import java.util.Objects;

public record StagsConfig(
        StorageConfig storage,
        SelectorConfig selector,
        RenderConfig render,
        AuthoringConfig authoring,
        CommandConfig command) {

    public StagsConfig {
        Objects.requireNonNull(storage, "storage");
        Objects.requireNonNull(selector, "selector");
        Objects.requireNonNull(render, "render");
        Objects.requireNonNull(authoring, "authoring");
        Objects.requireNonNull(command, "command");
    }
}
