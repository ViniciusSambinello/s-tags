package io.github.viniciussambinello.stags.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import io.github.viniciussambinello.stags.infrastructure.concurrent.MainThreadGuard;
import io.github.viniciussambinello.stags.infrastructure.concurrent.StorageExecutor;
import io.github.viniciussambinello.stags.infrastructure.config.ConfigDefaults;
import io.github.viniciussambinello.stags.infrastructure.config.FailurePolicy;
import io.github.viniciussambinello.stags.infrastructure.config.MySqlConfig;
import io.github.viniciussambinello.stags.infrastructure.config.StagsConfig;
import io.github.viniciussambinello.stags.infrastructure.config.StorageBackend;
import io.github.viniciussambinello.stags.infrastructure.config.StorageConfig;

@Tag("mysql-integration")
final class StorageFactoryTest {

    private MainThreadGuard offMainThreadGuard() {
        final org.bukkit.Server server = Mockito.mock(org.bukkit.Server.class);
        Mockito.when(server.isPrimaryThread()).thenReturn(false);
        return new MainThreadGuard(server);
    }

    private YamlConfiguration mysqlBackendConfig() {
        final YamlConfiguration source = new YamlConfiguration();
        source.set("storage.backend", "mysql");
        return source;
    }

    private StagsConfig configWith(final FailurePolicy failurePolicy) {
        final StagsConfig defaults = ConfigDefaults.buildDefault();
        final MySqlConfig unreachable = new MySqlConfig(
                "127.0.0.1", 18291, "stags", "root", "root", "stags_", 2, failurePolicy);
        final StorageConfig storage = new StorageConfig(StorageBackend.MYSQL, unreachable, defaults.storage().yaml());
        return new StagsConfig(storage, defaults.selector(), defaults.render(), defaults.authoring(), defaults.command());
    }

    @Test
    void abortPolicyThrowsWhenTheDatabaseIsUnreachable(@TempDir final Path dir) {
        try (StorageExecutor executor = new StorageExecutor()) {
            final StorageFactory factory = new StorageFactory(executor, offMainThreadGuard(), Logger.getLogger("test"));

            assertThrows(IllegalStateException.class,
                    () -> factory.create(dir, mysqlBackendConfig(), configWith(FailurePolicy.ABORT)));
        }
    }

    @Test
    void fallbackPolicyUsesYamlWhenTheDatabaseIsUnreachable(@TempDir final Path dir) {
        try (StorageExecutor executor = new StorageExecutor()) {
            final StorageFactory factory = new StorageFactory(executor, offMainThreadGuard(), Logger.getLogger("test"));

            final StorageBundle bundle = factory.create(dir, mysqlBackendConfig(), configWith(FailurePolicy.FALLBACK_YAML));

            assertEquals(StorageBackend.YAML, bundle.activeBackend());
            bundle.closeable().close();
        } catch (final Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
