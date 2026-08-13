package io.github.viniciussambinello.stags.infrastructure.storage;

import java.util.Arrays;
import java.util.Locale;

import org.bukkit.configuration.file.YamlConfiguration;

import io.github.viniciussambinello.stags.infrastructure.config.ConfigDefaults;
import io.github.viniciussambinello.stags.infrastructure.config.StorageBackend;

public final class StorageBackendResolver {

    public static StorageBackend resolveOrThrow(final YamlConfiguration source) {
        if (!source.isString("storage.backend")) {
            return ConfigDefaults.STORAGE_BACKEND;
        }
        final String raw = source.getString("storage.backend", "");
        try {
            return StorageBackend.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid storage.backend value '" + raw + "'. Accepted values: "
                    + Arrays.toString(StorageBackend.values()));
        }
    }

    private StorageBackendResolver() {
    }
}
