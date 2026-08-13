package io.github.viniciussambinello.stags.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

final class ConfigLoaderTest {

    private final ConfigLoader loader = new ConfigLoader();

    @Test
    void emptyFileFallsBackToDefaultsEverywhere() {
        final YamlConfiguration source = new YamlConfiguration();
        final ConfigLoader.LoadResult result = loader.load(source);
        assertEquals(ConfigDefaults.buildDefault(), result.config());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void outOfRangeIntFallsBackAndWarns() {
        final YamlConfiguration source = new YamlConfiguration();
        source.set("storage.mysql.port", -1);
        final ConfigLoader.LoadResult result = loader.load(source);
        assertEquals(ConfigDefaults.MYSQL_PORT, result.config().storage().mysql().port());
        assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("storage.mysql.port")));
    }

    @Test
    void wrongTypeFallsBackAndWarns() {
        final YamlConfiguration source = new YamlConfiguration();
        source.set("render.chat.enabled", "not-a-boolean");
        final ConfigLoader.LoadResult result = loader.load(source);
        assertEquals(ConfigDefaults.CHAT_RENDER_ENABLED, result.config().render().chat().enabled());
        assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("render.chat.enabled")));
    }

    @Test
    void missingKeysUseDocumentedDefaultsWithoutWarning() {
        final YamlConfiguration source = new YamlConfiguration();
        source.set("storage.backend", "MYSQL");
        final ConfigLoader.LoadResult result = loader.load(source);
        assertEquals(StorageBackend.MYSQL, result.config().storage().backend());
        assertEquals(ConfigDefaults.MYSQL_HOST, result.config().storage().mysql().host());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void unrecognizedKeyIsReported() {
        final YamlConfiguration source = new YamlConfiguration();
        source.set("storage.mysql.hostt", "typo");
        final ConfigLoader.LoadResult result = loader.load(source);
        assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("storage.mysql.hostt")));
    }

    @Test
    void menuSizeNotMultipleOfNineFallsBackAndWarns() {
        final YamlConfiguration source = new YamlConfiguration();
        source.set("selector.menu.size", 20);
        final ConfigLoader.LoadResult result = loader.load(source);
        assertEquals(ConfigDefaults.MENU_SIZE, result.config().selector().menu().size());
        assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("selector.menu.size")));
    }

    @Test
    void invalidEnumFallsBackAndWarns() {
        final YamlConfiguration source = new YamlConfiguration();
        source.set("selector.mode", "BOGUS");
        final ConfigLoader.LoadResult result = loader.load(source);
        assertEquals(ConfigDefaults.SELECTOR_MODE, result.config().selector().mode());
        assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("selector.mode")));
    }
}
