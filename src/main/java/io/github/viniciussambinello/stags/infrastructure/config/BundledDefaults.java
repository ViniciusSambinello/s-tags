package io.github.viniciussambinello.stags.infrastructure.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.bukkit.configuration.file.YamlConfiguration;

final class BundledDefaults {

    static YamlConfiguration load(final String resourceName) {
        try (InputStream stream = BundledDefaults.class.getResourceAsStream("/" + resourceName)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled resource: " + resourceName);
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    static void copyIfMissing(final Path dataFolder, final String resourceName) {
        final Path target = dataFolder.resolve(resourceName);
        if (Files.exists(target)) {
            return;
        }
        try {
            Files.createDirectories(dataFolder);
            try (InputStream stream = BundledDefaults.class.getResourceAsStream("/" + resourceName)) {
                if (stream == null) {
                    throw new IllegalStateException("Missing bundled resource: " + resourceName);
                }
                Files.copy(stream, target);
            }
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private BundledDefaults() {
    }
}
