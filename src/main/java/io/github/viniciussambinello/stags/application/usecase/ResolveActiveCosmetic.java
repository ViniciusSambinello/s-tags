package io.github.viniciussambinello.stags.application.usecase;

import java.util.Optional;

import io.github.viniciussambinello.stags.domain.catalogue.Catalogue;
import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.player.CosmeticOwnership;
import io.github.viniciussambinello.stags.domain.player.Selection;

public final class ResolveActiveCosmetic {

    public Optional<Cosmetic> resolve(
            final CosmeticKind kind,
            final Selection selection,
            final Catalogue catalogue,
            final CosmeticOwnership ownership) {
        return switch (selection) {
            case Selection.Active active -> resolveActive(kind, active, catalogue, ownership);
            case Selection.Cleared cleared -> Optional.empty();
            case Selection.Unset unset -> resolveUnset(kind, catalogue, ownership);
        };
    }

    private Optional<Cosmetic> resolveActive(
            final CosmeticKind kind,
            final Selection.Active active,
            final Catalogue catalogue,
            final CosmeticOwnership ownership) {
        final Optional<Cosmetic> selected = catalogue.find(kind, active.cosmeticId())
                .filter(cosmetic -> ownership.owns(cosmetic.id()));
        if (selected.isPresent()) {
            return selected;
        }
        return kind == CosmeticKind.TAG ? highestOwned(kind, catalogue, ownership) : Optional.empty();
    }

    private Optional<Cosmetic> resolveUnset(
            final CosmeticKind kind,
            final Catalogue catalogue,
            final CosmeticOwnership ownership) {
        return kind == CosmeticKind.TAG ? highestOwned(kind, catalogue, ownership) : Optional.empty();
    }

    private Optional<Cosmetic> highestOwned(
            final CosmeticKind kind,
            final Catalogue catalogue,
            final CosmeticOwnership ownership) {
        return catalogue.ownedBy(kind, ownership).stream().findFirst();
    }
}
