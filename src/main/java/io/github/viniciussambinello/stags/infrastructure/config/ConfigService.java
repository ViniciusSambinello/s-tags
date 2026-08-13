package io.github.viniciussambinello.stags.infrastructure.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ConfigService {

    public sealed interface ReloadOutcome {

        record Success(boolean storageRestartRequired) implements ReloadOutcome {
        }

        record Failure(String reason) implements ReloadOutcome {
        }
    }

    private final Path dataFolder;
    private final Logger logger;
    private final ConfigLoader configLoader;
    private final AtomicReference<StagsConfig> config;
    private final AtomicReference<MessageCatalog> messages;

    private ConfigService(
            final Path dataFolder,
            final Logger logger,
            final ConfigLoader configLoader,
            final StagsConfig initialConfig,
            final MessageCatalog initialMessages) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.configLoader = configLoader;
        this.config = new AtomicReference<>(initialConfig);
        this.messages = new AtomicReference<>(initialMessages);
    }

    public static ConfigService initial(final Path dataFolder, final Logger logger) {
        BundledDefaults.copyIfMissing(dataFolder, "config.yml");
        BundledDefaults.copyIfMissing(dataFolder, "messages.yml");

        final ConfigLoader configLoader = new ConfigLoader();
        final YamlConfiguration liveConfig = loadYaml(dataFolder.resolve("config.yml"));
        final ConfigLoader.LoadResult configResult = configLoader.load(liveConfig);
        configResult.warnings().forEach(logger::warning);

        final YamlConfiguration liveMessages = loadYaml(dataFolder.resolve("messages.yml"));
        final YamlConfiguration defaultMessages = BundledDefaults.load("messages.yml");
        final MessageCatalog.LoadResult messageResult = MessageCatalog.load(liveMessages, defaultMessages);
        messageResult.warnings().forEach(logger::warning);

        return new ConfigService(dataFolder, logger, configLoader, configResult.config(), messageResult.catalog());
    }

    public StagsConfig config() {
        return config.get();
    }

    public MessageCatalog messages() {
        return messages.get();
    }

    public ReloadOutcome reload() {
        final StagsConfig previous = config.get();
        try {
            final YamlConfiguration liveConfig = loadYaml(dataFolder.resolve("config.yml"));
            final ConfigLoader.LoadResult configResult = configLoader.load(liveConfig);

            final YamlConfiguration liveMessages = loadYaml(dataFolder.resolve("messages.yml"));
            final YamlConfiguration defaultMessages = BundledDefaults.load("messages.yml");
            final MessageCatalog.LoadResult messageResult = MessageCatalog.load(liveMessages, defaultMessages);

            logWarnings(configResult.warnings());
            logWarnings(messageResult.warnings());

            config.set(configResult.config());
            messages.set(messageResult.catalog());

            final boolean storageRestartRequired = !previous.storage().equals(configResult.config().storage());
            return new ReloadOutcome.Success(storageRestartRequired);
        } catch (final RuntimeException exception) {
            logger.log(Level.WARNING, "Reload failed, the previous configuration remains active", exception);
            return new ReloadOutcome.Failure(String.valueOf(exception.getMessage()));
        }
    }

    private void logWarnings(final List<String> warnings) {
        warnings.forEach(logger::warning);
    }

    private static YamlConfiguration loadYaml(final Path path) {
        final YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.load(path.toFile());
        } catch (final IOException | InvalidConfigurationException exception) {
            throw new IllegalStateException("Failed to load " + path.getFileName() + ": " + exception.getMessage(), exception);
        }
        return configuration;
    }
}
