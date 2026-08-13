package io.github.viniciussambinello.stags.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import io.github.viniciussambinello.stags.application.port.PermissionOracle;
import io.github.viniciussambinello.stags.application.usecase.OwnershipCheck;
import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.player.PlayerCosmetics;
import io.github.viniciussambinello.stags.domain.player.Selection;

public final class SelectorService {

    public record Entry(Cosmetic cosmetic, boolean owned, boolean active) {
    }

    private final CatalogueService catalogueService;
    private final PlayerCosmeticService playerCosmeticService;
    private final PermissionOracle permissionOracle;

    public SelectorService(
            final CatalogueService catalogueService,
            final PlayerCosmeticService playerCosmeticService,
            final PermissionOracle permissionOracle) {
        this.catalogueService = catalogueService;
        this.playerCosmeticService = playerCosmeticService;
        this.permissionOracle = permissionOracle;
    }

    public List<Entry> entries(final UUID playerId, final CosmeticKind kind, final boolean hideLocked) {
        final PlayerCosmetics playerCosmetics = playerCosmeticService.cached(playerId).orElse(PlayerCosmetics.unset(playerId));
        final Optional<CosmeticId> activeId = activeId(playerCosmetics.selection(kind));

        Stream<Entry> stream = catalogueService.catalogue().all(kind).stream()
                .map(cosmetic -> new Entry(
                        cosmetic,
                        OwnershipCheck.owns(permissionOracle, playerId, cosmetic),
                        activeId.map(id -> id.equals(cosmetic.id())).orElse(false)));
        if (hideLocked) {
            stream = stream.filter(Entry::owned);
        }
        return stream.toList();
    }

    public boolean hasActiveSelection(final UUID playerId, final CosmeticKind kind) {
        final PlayerCosmetics playerCosmetics = playerCosmeticService.cached(playerId).orElse(PlayerCosmetics.unset(playerId));
        return playerCosmetics.selection(kind) instanceof Selection.Active;
    }

    private Optional<CosmeticId> activeId(final Selection selection) {
        return selection instanceof Selection.Active active ? Optional.of(active.cosmeticId()) : Optional.empty();
    }
}
