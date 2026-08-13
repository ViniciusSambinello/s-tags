package io.github.viniciussambinello.stags.infrastructure.storage.yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

final class YamlFiles {

    static YamlConfiguration loadOrEmpty(final Path path) {
        if (!Files.exists(path)) {
            return new YamlConfiguration();
        }
        final YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.load(path.toFile());
        } catch (final IOException | InvalidConfigurationException exception) {
            throw new IllegalStateException("Failed to load " + path.getFileName() + ": " + exception.getMessage(), exception);
        }
        return configuration;
    }

    private YamlFiles() {
    }
}
