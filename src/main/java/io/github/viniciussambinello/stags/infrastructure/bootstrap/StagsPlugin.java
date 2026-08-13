package io.github.viniciussambinello.stags.infrastructure.bootstrap;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.viniciussambinello.stags.application.service.CatalogueService;
import io.github.viniciussambinello.stags.application.service.PlayerCosmeticService;
import io.github.viniciussambinello.stags.infrastructure.concurrent.MainThreadGuard;
import io.github.viniciussambinello.stags.infrastructure.concurrent.StorageExecutor;
import io.github.viniciussambinello.stags.infrastructure.config.ConfigService;
import io.github.viniciussambinello.stags.infrastructure.storage.StorageBundle;
import io.github.viniciussambinello.stags.infrastructure.storage.StorageFactory;

public final class StagsPlugin extends JavaPlugin {

    private final StorageExecutor storageExecutor;
    private final ConfigService configService;
    private final StorageBundle storageBundle;
    private final CatalogueService catalogueService;
    private final PlayerCosmeticService playerCosmeticService;

    public StagsPlugin() {
        this.storageExecutor = new StorageExecutor();
        this.configService = ConfigService.initial(getDataFolder().toPath(), getLogger());

        final MainThreadGuard guard = new MainThreadGuard(getServer());
        final StorageFactory storageFactory = new StorageFactory(storageExecutor, guard, getLogger());
        final YamlConfiguration rawConfig = YamlConfiguration.loadConfiguration(
                getDataFolder().toPath().resolve("config.yml").toFile());
        this.storageBundle = storageFactory.create(getDataFolder().toPath(), rawConfig, configService.config());

        this.catalogueService = new CatalogueService(storageBundle.cosmeticRepository());
        this.playerCosmeticService = new PlayerCosmeticService(storageBundle.selectionRepository());
    }

    @Override
    public void onEnable() {
        catalogueService.loadInitial().whenComplete((unused, throwable) -> {
            if (throwable != null) {
                getLogger().severe("Failed to load the cosmetic catalogue: " + throwable.getMessage());
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            getLogger().info("s-tags " + getPluginMeta().getVersion() + " enabled using the "
                    + storageBundle.activeBackend() + " storage backend.");
        }).join();
    }

    @Override
    public void onDisable() {
        try {
            storageBundle.closeable().close();
        } catch (final Exception exception) {
            getLogger().warning("Failed to close storage cleanly: " + exception.getMessage());
        }
        storageExecutor.close();
        getLogger().info("s-tags " + getPluginMeta().getVersion() + " disabled.");
    }

    public CatalogueService catalogueService() {
        return catalogueService;
    }

    public PlayerCosmeticService playerCosmeticService() {
        return playerCosmeticService;
    }

    public ConfigService configService() {
        return configService;
    }
}
