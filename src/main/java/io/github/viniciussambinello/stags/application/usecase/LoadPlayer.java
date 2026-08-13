package io.github.viniciussambinello.stags.application.usecase;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.github.viniciussambinello.stags.application.service.PlayerCosmeticService;
import io.github.viniciussambinello.stags.domain.player.PlayerCosmetics;

public final class LoadPlayer {

    private final PlayerCosmeticService playerCosmeticService;

    public LoadPlayer(final PlayerCosmeticService playerCosmeticService) {
        this.playerCosmeticService = playerCosmeticService;
    }

    public CompletableFuture<PlayerCosmetics> execute(final UUID playerId) {
        return playerCosmeticService.load(playerId);
    }
}
