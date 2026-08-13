package io.github.viniciussambinello.stags.infrastructure.storage;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;

import io.github.viniciussambinello.stags.infrastructure.config.FailurePolicy;
import io.github.viniciussambinello.stags.infrastructure.config.MySqlConfig;
import io.github.viniciussambinello.stags.infrastructure.config.StagsConfig;
import io.github.viniciussambinello.stags.infrastructure.config.StorageBackend;
import io.github.viniciussambinello.stags.infrastructure.concurrent.MainThreadGuard;
import io.github.viniciussambinello.stags.infrastructure.concurrent.StorageExecutor;
import io.github.viniciussambinello.stags.infrastructure.storage.mysql.HikariConnectionProvider;
import io.github.viniciussambinello.stags.infrastructure.storage.mysql.MySqlCosmeticRepository;
import io.github.viniciussambinello.stags.infrastructure.storage.mysql.MySqlSelectionRepository;
import io.github.viniciussambinello.stags.infrastructure.storage.mysql.SchemaMigrator;
import io.github.viniciussambinello.stags.infrastructure.storage.yaml.YamlCosmeticRepository;
import io.github.viniciussambinello.stags.infrastructure.storage.yaml.YamlSelectionRepository;

public final class StorageFactory {

    private final StorageExecutor executor;
    private final MainThreadGuard guard;
    private final Logger logger;

    public StorageFactory(final StorageExecutor executor, final MainThreadGuard guard, final Logger logger) {
        this.executor = executor;
        this.guard = guard;
        this.logger = logger;
    }

    public StorageBundle create(final Path dataFolder, final YamlConfiguration rawConfig, final StagsConfig config) {
        final StorageBackend backend = StorageBackendResolver.resolveOrThrow(rawConfig);
        if (backend == StorageBackend.YAML) {
            return createYaml(dataFolder, config);
        }
        return createMysqlOrFallback(dataFolder, config);
    }

    private StorageBundle createYaml(final Path dataFolder, final StagsConfig config) {
        final Duration interval = Duration.ofSeconds(config.storage().yaml().writeIntervalSeconds());
        final YamlCosmeticRepository cosmetics = new YamlCosmeticRepository(
                dataFolder.resolve("cosmetics.yml"), executor, guard, interval);
        final YamlSelectionRepository selections = new YamlSelectionRepository(
                dataFolder.resolve("selections.yml"), executor, guard, interval);
        return new StorageBundle(StorageBackend.YAML, cosmetics, selections, () -> {
            cosmetics.close();
            selections.close();
        });
    }

    private StorageBundle createMysqlOrFallback(final Path dataFolder, final StagsConfig config) {
        final MySqlConfig mysqlConfig = config.storage().mysql();
        final HikariConnectionProvider connectionProvider = new HikariConnectionProvider(mysqlConfig);
        try {
            connectionProvider.testConnectivity();
            new SchemaMigrator(connectionProvider, mysqlConfig.tablePrefix()).migrate();
            logger.info("Connected to the MySQL storage backend.");
            final MySqlCosmeticRepository cosmetics = new MySqlCosmeticRepository(
                    connectionProvider, executor, guard, mysqlConfig.tablePrefix());
            final MySqlSelectionRepository selections = new MySqlSelectionRepository(
                    connectionProvider, executor, guard, mysqlConfig.tablePrefix());
            return new StorageBundle(StorageBackend.MYSQL, cosmetics, selections, connectionProvider);
        } catch (final SQLException exception) {
            connectionProvider.close();
            if (mysqlConfig.failurePolicy() == FailurePolicy.FALLBACK_YAML) {
                logger.warning("MySQL storage backend unreachable (" + exception.getMessage()
                        + "), falling back to the YAML storage backend as configured.");
                return createYaml(dataFolder, config);
            }
            throw new IllegalStateException(
                    "MySQL storage backend unreachable and storage.mysql.failure-policy is ABORT: " + exception.getMessage(),
                    exception);
        }
    }
}
