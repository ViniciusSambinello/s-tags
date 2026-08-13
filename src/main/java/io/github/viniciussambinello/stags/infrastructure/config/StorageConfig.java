package io.github.viniciussambinello.stags.infrastructure.config;

import java.util.Objects;

public record StorageConfig(StorageBackend backend, MySqlConfig mysql, YamlStorageConfig yaml) {

    public StorageConfig {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(mysql, "mysql");
        Objects.requireNonNull(yaml, "yaml");
    }
}
