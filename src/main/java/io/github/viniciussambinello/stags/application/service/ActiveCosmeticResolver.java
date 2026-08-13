package io.github.viniciussambinello.stags.application.service;

import java.util.Optional;
import java.util.UUID;

import io.github.viniciussambinello.stags.application.port.PermissionOracle;
import io.github.viniciussambinello.stags.application.usecase.OwnershipCheck;
import io.github.viniciussambinello.stags.application.usecase.ResolveActiveCosmetic;
import io.github.viniciussambinello.stags.domain.catalogue.Catalogue;
import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.player.CosmeticOwnership;
import io.github.viniciussambinello.stags.domain.player.PlayerCosmetics;

public final class ActiveCosmeticResolver {

    private final CatalogueService catalogueService;
    private final PlayerCosmeticService playerCosmeticService;
    private final PermissionOracle permissionOracle;
    private final ResolveActiveCosmetic resolveActiveCosmetic;

    public ActiveCosmeticResolver(
            final CatalogueService catalogueService,
            final PlayerCosmeticService playerCosmeticService,
            final PermissionOracle permissionOracle) {
        this.catalogueService = catalogueService;
        this.playerCosmeticService = playerCosmeticService;
        this.permissionOracle = permissionOracle;
        this.resolveActiveCosmetic = new ResolveActiveCosmetic();
    }

    public Optional<Cosmetic> activeCosmetic(final UUID playerId, final CosmeticKind kind) {
        final Optional<PlayerCosmetics> cached = playerCosmeticService.cached(playerId);
        if (cached.isEmpty()) {
            return Optional.empty();
        }
        final Catalogue catalogue = catalogueService.catalogue();
        final CosmeticOwnership ownership = OwnershipCheck.asOwnership(permissionOracle, playerId, catalogue, kind);
        return resolveActiveCosmetic.resolve(kind, cached.get().selection(kind), catalogue, ownership);
    }
}
