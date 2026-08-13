package io.github.viniciussambinello.stags.infrastructure.placeholder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.github.viniciussambinello.stags.application.service.ActiveCosmeticResolver;
import io.github.viniciussambinello.stags.application.service.CatalogueService;
import io.github.viniciussambinello.stags.application.service.PlayerCosmeticService;
import io.github.viniciussambinello.stags.domain.cosmetic.Prefix;
import io.github.viniciussambinello.stags.testkit.InMemoryCosmeticRepository;
import io.github.viniciussambinello.stags.testkit.InMemorySelectionRepository;
import io.github.viniciussambinello.stags.testkit.StubPermissionOracle;
import net.kyori.adventure.text.Component;

final class PlaceholderApiIntegrationTest {

    @Test
    void activateIfPresentDoesNothingWhenPlaceholderApiIsAbsent() {
        final Server server = Mockito.mock(Server.class);
        final PluginManager pluginManager = Mockito.mock(PluginManager.class);
        Mockito.when(server.getPluginManager()).thenReturn(pluginManager);
        Mockito.when(pluginManager.isPluginEnabled("PlaceholderAPI")).thenReturn(false);

        final PlaceholderResolverHolder resolverHolder = new PlaceholderResolverHolder();
        final PlaceholderApiIntegration integration = newIntegration(server, resolverHolder);

        integration.activateIfPresent();

        final Prefix prefix = Prefix.parse("<gold>[VIP]</gold>");
        assertEquals(prefix.rendered(), resolverHolder.resolve(prefix, UUID.randomUUID()));
    }

    @Test
    void onPluginEnableIgnoresUnrelatedPlugins() {
        final Server server = Mockito.mock(Server.class);
        final PlaceholderResolverHolder resolverHolder = new PlaceholderResolverHolder();
        final PlaceholderApiIntegration integration = newIntegration(server, resolverHolder);

        integration.handlePluginEnabled("SomeOtherPlugin");

        final Prefix prefix = Prefix.parse("<gold>[VIP]</gold>");
        assertEquals(prefix.rendered(), resolverHolder.resolve(prefix, UUID.randomUUID()));
    }

    @Test
    void onPluginDisableFallsBackToLiteralRenderingWhenPlaceholderApiStops() {
        final Server server = Mockito.mock(Server.class);
        final PlaceholderResolverHolder resolverHolder = new PlaceholderResolverHolder();
        final Component replacement = Component.text("resolved");
        resolverHolder.activate((prefix, playerId) -> replacement);
        final PlaceholderApiIntegration integration = newIntegration(server, resolverHolder);

        integration.handlePluginDisabled("PlaceholderAPI");

        final Prefix prefix = Prefix.parse("<gold>[VIP]</gold>");
        assertEquals(prefix.rendered(), resolverHolder.resolve(prefix, UUID.randomUUID()));
    }

    @Test
    void onPluginDisableIgnoresUnrelatedPlugins() {
        final Server server = Mockito.mock(Server.class);
        final PlaceholderResolverHolder resolverHolder = new PlaceholderResolverHolder();
        final Component replacement = Component.text("resolved");
        resolverHolder.activate((prefix, playerId) -> replacement);
        final PlaceholderApiIntegration integration = newIntegration(server, resolverHolder);

        integration.handlePluginDisabled("SomeOtherPlugin");

        assertEquals(replacement, resolverHolder.resolve(Prefix.parse("<gold>[VIP]</gold>"), UUID.randomUUID()));
    }

    private PlaceholderApiIntegration newIntegration(final Server server, final PlaceholderResolverHolder resolverHolder) {
        final CatalogueService catalogueService = new CatalogueService(new InMemoryCosmeticRepository());
        final PlayerCosmeticService playerCosmeticService = new PlayerCosmeticService(new InMemorySelectionRepository());
        final ActiveCosmeticResolver activeCosmeticResolver =
                new ActiveCosmeticResolver(catalogueService, playerCosmeticService, new StubPermissionOracle());
        return new PlaceholderApiIntegration(
                server, Logger.getLogger("s-tags-test"), resolverHolder, "0.1.0",
                activeCosmeticResolver, catalogueService, new StubPermissionOracle());
    }
}
