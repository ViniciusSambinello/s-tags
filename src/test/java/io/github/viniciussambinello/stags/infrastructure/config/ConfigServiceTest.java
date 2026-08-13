package io.github.viniciussambinello.stags.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ConfigServiceTest {

    private final Logger logger = Logger.getLogger(ConfigServiceTest.class.getName());

    @Test
    void reloadPicksUpEditedValues(@TempDir final Path dataFolder) throws IOException {
        final ConfigService service = ConfigService.initial(dataFolder, logger);
        assertEquals(StorageBackend.YAML, service.config().storage().backend());

        final Path configFile = dataFolder.resolve("config.yml");
        final String content = Files.readString(configFile, StandardCharsets.UTF_8)
                .replace("backend: YAML", "backend: MYSQL");
        Files.writeString(configFile, content, StandardCharsets.UTF_8);

        final ConfigService.ReloadOutcome outcome = service.reload();
        assertInstanceOf(ConfigService.ReloadOutcome.Success.class, outcome);
        assertTrue(((ConfigService.ReloadOutcome.Success) outcome).storageRestartRequired());
        assertEquals(StorageBackend.MYSQL, service.config().storage().backend());
    }

    @Test
    void reloadWithUnreadableFileKeepsPreviousConfigActive(@TempDir final Path dataFolder) throws IOException {
        final ConfigService service = ConfigService.initial(dataFolder, logger);
        final StagsConfig before = service.config();

        final Path configFile = dataFolder.resolve("config.yml");
        Files.writeString(configFile, "storage:\n  backend: YAML\n    bad-indent: [unterminated\n", StandardCharsets.UTF_8);

        final ConfigService.ReloadOutcome outcome = service.reload();
        assertInstanceOf(ConfigService.ReloadOutcome.Failure.class, outcome);
        assertEquals(before, service.config());
    }

    @Test
    void noStorageChangeDoesNotRequireRestart(@TempDir final Path dataFolder) throws IOException {
        final ConfigService service = ConfigService.initial(dataFolder, logger);

        final Path messagesFile = dataFolder.resolve("messages.yml");
        final String content = Files.readString(messagesFile, StandardCharsets.UTF_8)
                .replace("Configuration reloaded.", "Config reloaded!");
        Files.writeString(messagesFile, content, StandardCharsets.UTF_8);

        final ConfigService.ReloadOutcome outcome = service.reload();
        assertInstanceOf(ConfigService.ReloadOutcome.Success.class, outcome);
        assertTrue(!((ConfigService.ReloadOutcome.Success) outcome).storageRestartRequired());
    }
}
