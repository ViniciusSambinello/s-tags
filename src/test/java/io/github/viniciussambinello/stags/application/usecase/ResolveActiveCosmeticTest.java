package io.github.viniciussambinello.stags.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.viniciussambinello.stags.domain.catalogue.Catalogue;
import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.cosmetic.PermissionNode;
import io.github.viniciussambinello.stags.domain.cosmetic.Prefix;
import io.github.viniciussambinello.stags.domain.cosmetic.Weight;
import io.github.viniciussambinello.stags.domain.player.CosmeticOwnership;
import io.github.viniciussambinello.stags.domain.player.Selection;

final class ResolveActiveCosmeticTest {

    private final ResolveActiveCosmetic useCase = new ResolveActiveCosmetic();

    private static Cosmetic cosmetic(final CosmeticKind kind, final String id, final int weight) {
        return new Cosmetic(
                kind,
                new CosmeticId(id),
                Prefix.parse("<gray>[" + id + "]</gray>"),
                PermissionNode.NONE,
                new Weight(weight));
    }

    private static CosmeticOwnership ownershipOf(final String... ownedIds) {
        final Set<CosmeticId> owned = Set.of(ownedIds).stream().map(CosmeticId::new).collect(java.util.stream.Collectors.toSet());
        return owned::contains;
    }

    @Test
    void tagWithNoSelectionDefaultsToHighestOwnedWeight() {
        final Catalogue catalogue = Catalogue.of(List.of(
                cosmetic(CosmeticKind.TAG, "vip", 100),
                cosmetic(CosmeticKind.TAG, "member", 10)));

        final Optional<Cosmetic> resolved = useCase.resolve(
                CosmeticKind.TAG, Selection.UNSET, catalogue, ownershipOf("vip", "member"));

        assertTrue(resolved.isPresent());
        assertEquals("vip", resolved.get().id().value());
    }

    @Test
    void tagWithNoOwnedCosmeticsResolvesEmpty() {
        final Catalogue catalogue = Catalogue.of(List.of(cosmetic(CosmeticKind.TAG, "vip", 100)));
        final Optional<Cosmetic> resolved = useCase.resolve(
                CosmeticKind.TAG, Selection.UNSET, catalogue, ownershipOf());
        assertTrue(resolved.isEmpty());
    }

    @Test
    void tagFallsBackWhenActiveSelectionIsDeleted() {
        final Catalogue catalogue = Catalogue.of(List.of(cosmetic(CosmeticKind.TAG, "member", 10)));
        final Selection selection = new Selection.Active(new CosmeticId("vip"));

        final Optional<Cosmetic> resolved = useCase.resolve(
                CosmeticKind.TAG, selection, catalogue, ownershipOf("member"));

        assertTrue(resolved.isPresent());
        assertEquals("member", resolved.get().id().value());
    }

    @Test
    void tagFallsBackWhenPermissionIsRevoked() {
        final Catalogue catalogue = Catalogue.of(List.of(
                cosmetic(CosmeticKind.TAG, "vip", 100),
                cosmetic(CosmeticKind.TAG, "member", 10)));
        final Selection selection = new Selection.Active(new CosmeticId("vip"));

        final Optional<Cosmetic> resolved = useCase.resolve(
                CosmeticKind.TAG, selection, catalogue, ownershipOf("member"));

        assertTrue(resolved.isPresent());
        assertEquals("member", resolved.get().id().value());
    }

    @Test
    void tiedWeightBreaksByAscendingIdentifier() {
        final Catalogue catalogue = Catalogue.of(List.of(
                cosmetic(CosmeticKind.TAG, "beta", 100),
                cosmetic(CosmeticKind.TAG, "alpha", 100)));

        final Optional<Cosmetic> resolved = useCase.resolve(
                CosmeticKind.TAG, Selection.UNSET, catalogue, ownershipOf("alpha", "beta"));

        assertTrue(resolved.isPresent());
        assertEquals("alpha", resolved.get().id().value());
    }

    @Test
    void titleWithNoSelectionResolvesEmptyRatherThanDefaulting() {
        final Catalogue catalogue = Catalogue.of(List.of(cosmetic(CosmeticKind.TITLE, "champion", 500)));

        final Optional<Cosmetic> resolved = useCase.resolve(
                CosmeticKind.TITLE, Selection.UNSET, catalogue, ownershipOf("champion"));

        assertTrue(resolved.isEmpty());
    }

    @Test
    void titleIsClearedRatherThanReplacedWhenPermissionRevoked() {
        final Catalogue catalogue = Catalogue.of(List.of(
                cosmetic(CosmeticKind.TITLE, "champion", 500),
                cosmetic(CosmeticKind.TITLE, "veteran", 10)));
        final Selection selection = new Selection.Active(new CosmeticId("champion"));

        final Optional<Cosmetic> resolved = useCase.resolve(
                CosmeticKind.TITLE, selection, catalogue, ownershipOf("veteran"));

        assertTrue(resolved.isEmpty());
    }

    @Test
    void clearedSelectionAlwaysResolvesEmpty() {
        final Catalogue catalogue = Catalogue.of(List.of(cosmetic(CosmeticKind.TAG, "vip", 100)));
        final Optional<Cosmetic> resolved = useCase.resolve(
                CosmeticKind.TAG, Selection.CLEARED, catalogue, ownershipOf("vip"));
        assertTrue(resolved.isEmpty());
    }
}
