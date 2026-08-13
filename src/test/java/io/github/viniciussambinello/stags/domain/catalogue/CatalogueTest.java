package io.github.viniciussambinello.stags.domain.catalogue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.cosmetic.PermissionNode;
import io.github.viniciussambinello.stags.domain.cosmetic.Prefix;
import io.github.viniciussambinello.stags.domain.cosmetic.Weight;

final class CatalogueTest {

    private static Cosmetic tag(final String id, final int weight) {
        return new Cosmetic(
                CosmeticKind.TAG,
                new CosmeticId(id),
                Prefix.parse("<gray>[" + id + "]</gray>"),
                PermissionNode.NONE,
                new Weight(weight));
    }

    @Test
    void ordersByDescendingWeightThenAscendingIdentifier() {
        final Catalogue catalogue = Catalogue.of(List.of(
                tag("member", 10),
                tag("vip", 100),
                tag("beta", 100),
                tag("alpha", 100)));

        final List<CosmeticId> order = catalogue.all(CosmeticKind.TAG).stream().map(Cosmetic::id).toList();
        assertEquals(
                List.of(new CosmeticId("alpha"), new CosmeticId("beta"), new CosmeticId("vip"), new CosmeticId("member")),
                order);
    }

    @Test
    void withCosmeticReplacesExistingByKindAndId() {
        final Catalogue original = Catalogue.of(List.of(tag("vip", 100)));
        final Catalogue updated = original.withCosmetic(tag("vip", 500));

        assertEquals(1, updated.all(CosmeticKind.TAG).size());
        assertEquals(500, updated.all(CosmeticKind.TAG).get(0).weight().value());
        assertEquals(100, original.all(CosmeticKind.TAG).get(0).weight().value());
    }

    @Test
    void withoutCosmeticRemovesOnlyMatchingEntry() {
        final Catalogue catalogue = Catalogue.of(List.of(tag("vip", 100), tag("member", 10)))
                .withoutCosmetic(CosmeticKind.TAG, new CosmeticId("vip"));

        assertFalse(catalogue.contains(CosmeticKind.TAG, new CosmeticId("vip")));
        assertTrue(catalogue.contains(CosmeticKind.TAG, new CosmeticId("member")));
    }

    @Test
    void namespacesAreIndependentPerKind() {
        final Cosmetic tag = tag("vip", 100);
        final Cosmetic title = new Cosmetic(
                CosmeticKind.TITLE,
                new CosmeticId("vip"),
                Prefix.parse("<gold>Champion</gold>"),
                PermissionNode.NONE,
                new Weight(100));

        final Catalogue catalogue = Catalogue.of(List.of(tag, title));

        assertTrue(catalogue.contains(CosmeticKind.TAG, new CosmeticId("vip")));
        assertTrue(catalogue.contains(CosmeticKind.TITLE, new CosmeticId("vip")));
        assertEquals(1, catalogue.all(CosmeticKind.TAG).size());
        assertEquals(1, catalogue.all(CosmeticKind.TITLE).size());
    }
}
