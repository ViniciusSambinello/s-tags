package io.github.viniciussambinello.stags.application.usecase;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.github.viniciussambinello.stags.application.port.CosmeticRenderer;
import io.github.viniciussambinello.stags.application.port.PermissionOracle;
import io.github.viniciussambinello.stags.application.service.CatalogueService;
import io.github.viniciussambinello.stags.application.service.PlayerCosmeticService;
import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.player.Selection;

public final class SelectCosmetic {

    public sealed interface Result {

        record Applied(Cosmetic cosmetic) implements Result {
        }

        record NotOwned() implements Result {
        }

        record UnknownCosmetic() implements Result {
        }

        record StorageFailure() implements Result {
        }
    }

    private final CatalogueService catalogueService;
    private final PlayerCosmeticService playerCosmeticService;
    private final PermissionOracle permissionOracle;
    private final CosmeticRenderer renderer;

    public SelectCosmetic(
            final CatalogueService catalogueService,
            final PlayerCosmeticService playerCosmeticService,
            final PermissionOracle permissionOracle,
            final CosmeticRenderer renderer) {
        this.catalogueService = catalogueService;
        this.playerCosmeticService = playerCosmeticService;
        this.permissionOracle = permissionOracle;
        this.renderer = renderer;
    }

    public CompletableFuture<Result> execute(final UUID playerId, final CosmeticKind kind, final CosmeticId cosmeticId) {
        final Optional<Cosmetic> target = catalogueService.catalogue().find(kind, cosmeticId);
        if (target.isEmpty()) {
            return CompletableFuture.completedFuture(new Result.UnknownCosmetic());
        }
        if (!OwnershipCheck.owns(permissionOracle, playerId, target.get())) {
            return CompletableFuture.completedFuture(new Result.NotOwned());
        }
        return playerCosmeticService.updateSelection(playerId, kind, new Selection.Active(cosmeticId))
                .thenApply(success -> {
                    if (!Boolean.TRUE.equals(success)) {
                        return new Result.StorageFailure();
                    }
                    renderer.refresh(playerId);
                    return new Result.Applied(target.get());
                });
    }
}
