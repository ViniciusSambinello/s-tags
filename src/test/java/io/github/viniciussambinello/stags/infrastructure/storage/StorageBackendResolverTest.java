package io.github.viniciussambinello.stags.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import io.github.viniciussambinello.stags.infrastructure.config.StorageBackend;

final class StorageBackendResolverTest {

    @Test
    void missingKeyDefaultsToYaml() {
        final YamlConfiguration source = new YamlConfiguration();
        assertEquals(StorageBackend.YAML, StorageBackendResolver.resolveOrThrow(source));
    }

    @Test
    void validValueIsResolved() {
        final YamlConfiguration source = new YamlConfiguration();
        source.set("storage.backend", "MYSQL");
        assertEquals(StorageBackend.MYSQL, StorageBackendResolver.resolveOrThrow(source));
    }

    @Test
    void unrecognizedValueAbortsRatherThanFallingBack() {
        final YamlConfiguration source = new YamlConfiguration();
        source.set("storage.backend", "MONGODB");
        final IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> StorageBackendResolver.resolveOrThrow(source));
        assertEquals(true, exception.getMessage().contains("MONGODB"));
        assertEquals(true, exception.getMessage().contains("YAML"));
        assertEquals(true, exception.getMessage().contains("MYSQL"));
    }
}
