package io.github.viniciussambinello.stags.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

final class BundledResourcesTest {

    @Test
    void bundledConfigYamlProducesNoWarnings() {
        final YamlConfiguration bundled = BundledDefaults.load("config.yml");
        final ConfigLoader.LoadResult result = new ConfigLoader().load(bundled);
        assertTrue(result.warnings().isEmpty(), () -> String.join("\n", result.warnings()));
    }

    @Test
    void bundledMessagesYamlProducesNoWarnings() {
        final YamlConfiguration bundled = BundledDefaults.load("messages.yml");
        final MessageCatalog.LoadResult result = MessageCatalog.load(bundled, bundled);
        assertTrue(result.warnings().isEmpty(), () -> String.join("\n", result.warnings()));
    }
}
