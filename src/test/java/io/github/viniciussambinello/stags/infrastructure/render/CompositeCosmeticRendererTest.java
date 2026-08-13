package io.github.viniciussambinello.stags.infrastructure.render;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class CompositeCosmeticRendererTest {

    @Test
    void refreshWithAnOnlinePlayerDelegatesToEveryTarget() {
        final UUID playerId = UUID.randomUUID();
        final Player player = Mockito.mock(Player.class);
        final Server server = Mockito.mock(Server.class);
        Mockito.when(server.getPlayer(playerId)).thenReturn(player);

        final NametagRenderAdapter nametag = Mockito.mock(NametagRenderAdapter.class);
        final TabListRenderAdapter tabList = Mockito.mock(TabListRenderAdapter.class);
        final TitleHologramRenderAdapter hologram = Mockito.mock(TitleHologramRenderAdapter.class);
        final CompositeCosmeticRenderer renderer = new CompositeCosmeticRenderer(server, nametag, tabList, hologram);

        renderer.refresh(playerId);

        Mockito.verify(nametag).refresh(player);
        Mockito.verify(tabList).refresh(player);
        Mockito.verify(hologram).refresh(player);
    }

    @Test
    void refreshWithAnOfflinePlayerDoesNothing() {
        final UUID playerId = UUID.randomUUID();
        final Server server = Mockito.mock(Server.class);
        Mockito.when(server.getPlayer(playerId)).thenReturn(null);

        final NametagRenderAdapter nametag = Mockito.mock(NametagRenderAdapter.class);
        final TabListRenderAdapter tabList = Mockito.mock(TabListRenderAdapter.class);
        final TitleHologramRenderAdapter hologram = Mockito.mock(TitleHologramRenderAdapter.class);
        final CompositeCosmeticRenderer renderer = new CompositeCosmeticRenderer(server, nametag, tabList, hologram);

        renderer.refresh(playerId);

        Mockito.verifyNoInteractions(nametag, tabList, hologram);
    }

    @Test
    void teardownDelegatesToEveryTarget() {
        final Player player = Mockito.mock(Player.class);
        final Server server = Mockito.mock(Server.class);
        final NametagRenderAdapter nametag = Mockito.mock(NametagRenderAdapter.class);
        final TabListRenderAdapter tabList = Mockito.mock(TabListRenderAdapter.class);
        final TitleHologramRenderAdapter hologram = Mockito.mock(TitleHologramRenderAdapter.class);
        final CompositeCosmeticRenderer renderer = new CompositeCosmeticRenderer(server, nametag, tabList, hologram);

        renderer.teardown(player);

        Mockito.verify(nametag).teardown(player);
        Mockito.verify(tabList).teardown(player);
        Mockito.verify(hologram).teardown(player);
    }

    @Test
    void shutdownDelegatesToEveryTarget() {
        final Server server = Mockito.mock(Server.class);
        final NametagRenderAdapter nametag = Mockito.mock(NametagRenderAdapter.class);
        final TabListRenderAdapter tabList = Mockito.mock(TabListRenderAdapter.class);
        final TitleHologramRenderAdapter hologram = Mockito.mock(TitleHologramRenderAdapter.class);
        final CompositeCosmeticRenderer renderer = new CompositeCosmeticRenderer(server, nametag, tabList, hologram);

        renderer.shutdown();

        Mockito.verify(nametag).shutdown();
        Mockito.verify(tabList).shutdown();
        Mockito.verify(hologram).shutdown();
    }

    @Test
    void onViewerJoinDelegatesToEveryTarget() {
        final Player viewer = Mockito.mock(Player.class);
        final Server server = Mockito.mock(Server.class);
        final NametagRenderAdapter nametag = Mockito.mock(NametagRenderAdapter.class);
        final TabListRenderAdapter tabList = Mockito.mock(TabListRenderAdapter.class);
        final TitleHologramRenderAdapter hologram = Mockito.mock(TitleHologramRenderAdapter.class);
        final CompositeCosmeticRenderer renderer = new CompositeCosmeticRenderer(server, nametag, tabList, hologram);

        renderer.onViewerJoin(viewer);

        Mockito.verify(nametag).onViewerJoin(viewer);
        Mockito.verify(tabList).onViewerJoin(viewer);
        Mockito.verify(hologram).onViewerJoin(viewer);
    }
}
