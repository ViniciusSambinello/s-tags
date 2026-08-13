package io.github.viniciussambinello.stags.infrastructure.bootstrap;

import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import io.github.viniciussambinello.stags.application.service.ActiveCosmeticResolver;
import io.github.viniciussambinello.stags.application.service.CatalogueService;
import io.github.viniciussambinello.stags.application.service.PlayerCosmeticService;
import io.github.viniciussambinello.stags.application.usecase.LoadPlayer;
import io.github.viniciussambinello.stags.infrastructure.concurrent.MainThreadDispatcher;
import io.github.viniciussambinello.stags.infrastructure.concurrent.MainThreadGuard;
import io.github.viniciussambinello.stags.infrastructure.concurrent.StorageExecutor;
import io.github.viniciussambinello.stags.infrastructure.config.ConfigService;
import io.github.viniciussambinello.stags.infrastructure.permission.BukkitPermissionOracle;
import io.github.viniciussambinello.stags.infrastructure.render.ChatRenderAdapter;
import io.github.viniciussambinello.stags.infrastructure.render.CompositeCosmeticRenderer;
import io.github.viniciussambinello.stags.infrastructure.render.NametagRenderAdapter;
import io.github.viniciussambinello.stags.infrastructure.render.PlayerSessionListener;
import io.github.viniciussambinello.stags.infrastructure.render.ReconciliationTask;
import io.github.viniciussambinello.stags.infrastructure.render.TabListRenderAdapter;
import io.github.viniciussambinello.stags.infrastructure.storage.StorageBundle;
import io.github.viniciussambinello.stags.infrastructure.storage.StorageFactory;

public final class StagsPlugin extends JavaPlugin {

    private final StorageExecutor storageExecutor;
    private final ConfigService configService;
    private final StorageBundle storageBundle;
    private final CatalogueService catalogueService;
    private final PlayerCosmeticService playerCosmeticService;
    private final BukkitPermissionOracle permissionOracle;
    private final ActiveCosmeticResolver activeCosmeticResolver;
    private final CompositeCosmeticRenderer compositeCosmeticRenderer;
    private final ChatRenderAdapter chatRenderAdapter;
    private final PlayerSessionListener playerSessionListener;
    private final ReconciliationTask reconciliationTask;
    private final AtomicReference<BukkitTask> scheduledReconciliation;

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
        this.permissionOracle = new BukkitPermissionOracle(getServer());
        this.activeCosmeticResolver = new ActiveCosmeticResolver(catalogueService, playerCosmeticService, permissionOracle);

        final NametagRenderAdapter nametagRenderAdapter =
                new NametagRenderAdapter(configService, activeCosmeticResolver, getServer());
        final TabListRenderAdapter tabListRenderAdapter = new TabListRenderAdapter(configService, activeCosmeticResolver);
        this.compositeCosmeticRenderer = new CompositeCosmeticRenderer(getServer(), nametagRenderAdapter, tabListRenderAdapter);
        this.chatRenderAdapter = new ChatRenderAdapter(configService, activeCosmeticResolver);

        final LoadPlayer loadPlayer = new LoadPlayer(playerCosmeticService);
        final MainThreadDispatcher dispatcher = new MainThreadDispatcher(this);
        this.playerSessionListener =
                new PlayerSessionListener(loadPlayer, playerCosmeticService, compositeCosmeticRenderer, dispatcher);
        this.reconciliationTask = new ReconciliationTask(getServer(), compositeCosmeticRenderer);
        this.scheduledReconciliation = new AtomicReference<>();
    }

    @Override
    public void onEnable() {
        catalogueService.loadInitial().whenComplete((unused, throwable) -> {
            if (throwable != null) {
                getLogger().severe("Failed to load the cosmetic catalogue: " + throwable.getMessage());
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            getServer().getPluginManager().registerEvents(chatRenderAdapter, this);
            getServer().getPluginManager().registerEvents(playerSessionListener, this);
            startOrStopReconciliation();
            getLogger().info("s-tags " + getPluginMeta().getVersion() + " enabled using the "
                    + storageBundle.activeBackend() + " storage backend.");
        }).join();
    }

    public void startOrStopReconciliation() {
        final BukkitTask existing = scheduledReconciliation.getAndSet(null);
        if (existing != null) {
            existing.cancel();
        }
        final var reconciliation = configService.config().render().reconciliation();
        if (reconciliation.enabled()) {
            final long intervalTicks = reconciliation.intervalSeconds() * 20L;
            scheduledReconciliation.set(
                    getServer().getScheduler().runTaskTimer(this, reconciliationTask, intervalTicks, intervalTicks));
        }
    }

    @Override
    public void onDisable() {
        final BukkitTask existing = scheduledReconciliation.getAndSet(null);
        if (existing != null) {
            existing.cancel();
        }
        getServer().getOnlinePlayers().forEach(compositeCosmeticRenderer::teardown);
        compositeCosmeticRenderer.shutdown();
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

    public CompositeCosmeticRenderer compositeCosmeticRenderer() {
        return compositeCosmeticRenderer;
    }
}
