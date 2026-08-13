package io.github.viniciussambinello.stags.infrastructure.placeholder;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

import io.github.viniciussambinello.stags.application.port.PermissionOracle;
import io.github.viniciussambinello.stags.application.service.ActiveCosmeticResolver;
import io.github.viniciussambinello.stags.application.service.CatalogueService;

public final class PlaceholderApiIntegration implements Listener {

    private static final String PLACEHOLDER_API_PLUGIN_NAME = "PlaceholderAPI";

    private final Server server;
    private final Logger logger;
    private final PlaceholderResolverHolder resolverHolder;
    private final String pluginVersion;
    private final ActiveCosmeticResolver activeCosmeticResolver;
    private final CatalogueService catalogueService;
    private final PermissionOracle permissionOracle;
    private final AtomicReference<StagsExpansion> registeredExpansion;

    public PlaceholderApiIntegration(
            final Server server,
            final Logger logger,
            final PlaceholderResolverHolder resolverHolder,
            final String pluginVersion,
            final ActiveCosmeticResolver activeCosmeticResolver,
            final CatalogueService catalogueService,
            final PermissionOracle permissionOracle) {
        this.server = server;
        this.logger = logger;
        this.resolverHolder = resolverHolder;
        this.pluginVersion = pluginVersion;
        this.activeCosmeticResolver = activeCosmeticResolver;
        this.catalogueService = catalogueService;
        this.permissionOracle = permissionOracle;
        this.registeredExpansion = new AtomicReference<>();
    }

    public void activateIfPresent() {
        if (server.getPluginManager().isPluginEnabled(PLACEHOLDER_API_PLUGIN_NAME)) {
            activate();
        }
    }

    @EventHandler
    public void onPluginEnable(final PluginEnableEvent event) {
        handlePluginEnabled(event.getPlugin().getName());
    }

    @EventHandler
    public void onPluginDisable(final PluginDisableEvent event) {
        handlePluginDisabled(event.getPlugin().getName());
    }

    void handlePluginEnabled(final String pluginName) {
        if (PLACEHOLDER_API_PLUGIN_NAME.equals(pluginName)) {
            activate();
        }
    }

    void handlePluginDisabled(final String pluginName) {
        if (PLACEHOLDER_API_PLUGIN_NAME.equals(pluginName)) {
            resolverHolder.deactivate();
            logger.info("PlaceholderAPI was disabled, s-tags placeholders will render literally.");
        }
    }

    private void activate() {
        resolverHolder.activate(new PlaceholderApiResolver(server));
        if (registeredExpansion.get() == null) {
            final StagsExpansion expansion =
                    new StagsExpansion(pluginVersion, activeCosmeticResolver, catalogueService, permissionOracle);
            if (registeredExpansion.compareAndSet(null, expansion)) {
                expansion.register();
            }
        }
        logger.info("PlaceholderAPI detected, placeholder integration is active.");
    }
}
