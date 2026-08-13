package io.github.viniciussambinello.stags.application.usecase;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.github.viniciussambinello.stags.application.port.CosmeticRenderer;
import io.github.viniciussambinello.stags.application.service.PlayerCosmeticService;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.player.Selection;

public final class ClearCosmetic {

    public sealed interface Result {

        record Cleared() implements Result {
        }

        record StorageFailure() implements Result {
        }
    }

    private final PlayerCosmeticService playerCosmeticService;
    private final CosmeticRenderer renderer;

    public ClearCosmetic(final PlayerCosmeticService playerCosmeticService, final CosmeticRenderer renderer) {
        this.playerCosmeticService = playerCosmeticService;
        this.renderer = renderer;
    }

    public CompletableFuture<Result> execute(final UUID playerId, final CosmeticKind kind) {
        return playerCosmeticService.updateSelection(playerId, kind, Selection.CLEARED)
                .thenApply(success -> {
                    if (!Boolean.TRUE.equals(success)) {
                        return new Result.StorageFailure();
                    }
                    renderer.refresh(playerId);
                    return new Result.Cleared();
                });
    }
}
