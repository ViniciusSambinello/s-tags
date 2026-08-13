package io.github.viniciussambinello.stags.application.usecase;

import java.util.UUID;

import io.github.viniciussambinello.stags.application.port.PermissionOracle;
import io.github.viniciussambinello.stags.domain.catalogue.Catalogue;
import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.player.CosmeticOwnership;

final class OwnershipCheck {

    static boolean owns(final PermissionOracle oracle, final UUID playerId, final Cosmetic cosmetic) {
        return cosmetic.permission().isEmpty() || oracle.hasPermission(playerId, cosmetic.permission().value());
    }

    static CosmeticOwnership asOwnership(
            final PermissionOracle oracle, final UUID playerId, final Catalogue catalogue, final CosmeticKind kind) {
        return cosmeticId -> catalogue.find(kind, cosmeticId)
                .map(cosmetic -> owns(oracle, playerId, cosmetic))
                .orElse(false);
    }

    private OwnershipCheck() {
    }
}
