package io.github.viniciussambinello.stags.infrastructure.render;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import io.github.viniciussambinello.stags.application.service.PlayerCosmeticService;
import io.github.viniciussambinello.stags.application.usecase.LoadPlayer;
import io.github.viniciussambinello.stags.infrastructure.concurrent.MainThreadDispatcher;

public final class PlayerSessionListener implements Listener {

    private final LoadPlayer loadPlayer;
    private final PlayerCosmeticService playerCosmeticService;
    private final CompositeCosmeticRenderer renderer;
    private final MainThreadDispatcher dispatcher;

    public PlayerSessionListener(
            final LoadPlayer loadPlayer,
            final PlayerCosmeticService playerCosmeticService,
            final CompositeCosmeticRenderer renderer,
            final MainThreadDispatcher dispatcher) {
        this.loadPlayer = loadPlayer;
        this.playerCosmeticService = playerCosmeticService;
        this.renderer = renderer;
        this.dispatcher = dispatcher;
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        loadPlayer.execute(player.getUniqueId()).thenRun(() -> dispatcher.run(() -> {
            if (player.isOnline()) {
                renderer.refresh(player.getUniqueId());
            }
        }));
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        renderer.teardown(player);
        playerCosmeticService.awaitPending(player.getUniqueId())
                .thenRun(() -> playerCosmeticService.release(player.getUniqueId()));
    }
}
