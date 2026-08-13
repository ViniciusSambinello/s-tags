package io.github.viniciussambinello.stags.infrastructure.render;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.github.viniciussambinello.stags.application.service.PlayerCosmeticService;
import io.github.viniciussambinello.stags.application.usecase.LoadPlayer;
import io.github.viniciussambinello.stags.domain.player.PlayerCosmetics;
import io.github.viniciussambinello.stags.domain.player.Selection;
import io.github.viniciussambinello.stags.infrastructure.concurrent.MainThreadDispatcher;
import io.github.viniciussambinello.stags.testkit.InMemorySelectionRepository;

final class PlayerSessionListenerTest {

    private MainThreadDispatcher synchronousDispatcher() {
        final MainThreadDispatcher dispatcher = Mockito.mock(MainThreadDispatcher.class);
        Mockito.doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(dispatcher).run(Mockito.any());
        return dispatcher;
    }

    @Test
    void joinRefreshesTheRendererAndNotifiesOnlineViewersWhenStillOnline() throws Exception {
        final UUID playerId = UUID.randomUUID();
        final InMemorySelectionRepository repository = new InMemorySelectionRepository();
        repository.put(new PlayerCosmetics(playerId, Selection.UNSET, Selection.UNSET));
        final PlayerCosmeticService playerCosmeticService = new PlayerCosmeticService(repository);
        final LoadPlayer loadPlayer = new LoadPlayer(playerCosmeticService);
        final CompositeCosmeticRenderer renderer = Mockito.mock(CompositeCosmeticRenderer.class);

        final PlayerSessionListener listener =
                new PlayerSessionListener(loadPlayer, playerCosmeticService, renderer, synchronousDispatcher());

        final Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(playerId);
        Mockito.when(player.isOnline()).thenReturn(true);

        listener.handleJoin(player);

        Mockito.verify(renderer).refresh(playerId);
        Mockito.verify(renderer).onViewerJoin(player);
    }

    @Test
    void joinDoesNothingWhenThePlayerAlreadyDisconnected() throws Exception {
        final UUID playerId = UUID.randomUUID();
        final InMemorySelectionRepository repository = new InMemorySelectionRepository();
        repository.put(new PlayerCosmetics(playerId, Selection.UNSET, Selection.UNSET));
        final PlayerCosmeticService playerCosmeticService = new PlayerCosmeticService(repository);
        final LoadPlayer loadPlayer = new LoadPlayer(playerCosmeticService);
        final CompositeCosmeticRenderer renderer = Mockito.mock(CompositeCosmeticRenderer.class);

        final PlayerSessionListener listener =
                new PlayerSessionListener(loadPlayer, playerCosmeticService, renderer, synchronousDispatcher());

        final Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(playerId);
        Mockito.when(player.isOnline()).thenReturn(false);

        listener.handleJoin(player);

        Mockito.verifyNoInteractions(renderer);
    }

    @Test
    void quitTearsDownRenderingAndReleasesTheCacheAfterPendingWritesSettle() throws Exception {
        final UUID playerId = UUID.randomUUID();
        final InMemorySelectionRepository repository = new InMemorySelectionRepository();
        repository.put(new PlayerCosmetics(playerId, Selection.UNSET, Selection.UNSET));
        final PlayerCosmeticService playerCosmeticService = new PlayerCosmeticService(repository);
        playerCosmeticService.load(playerId).get();
        final LoadPlayer loadPlayer = new LoadPlayer(playerCosmeticService);
        final CompositeCosmeticRenderer renderer = Mockito.mock(CompositeCosmeticRenderer.class);

        final PlayerSessionListener listener =
                new PlayerSessionListener(loadPlayer, playerCosmeticService, renderer, synchronousDispatcher());

        final Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(playerId);

        listener.handleQuit(player);

        Mockito.verify(renderer).teardown(player);
        assertTrue(playerCosmeticService.cached(playerId).isEmpty());
    }
}
