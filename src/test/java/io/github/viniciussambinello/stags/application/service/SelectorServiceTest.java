package io.github.viniciussambinello.stags.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.cosmetic.PermissionNode;
import io.github.viniciussambinello.stags.domain.cosmetic.Prefix;
import io.github.viniciussambinello.stags.domain.cosmetic.Weight;
import io.github.viniciussambinello.stags.domain.player.PlayerCosmetics;
import io.github.viniciussambinello.stags.domain.player.Selection;
import io.github.viniciussambinello.stags.testkit.InMemoryCosmeticRepository;
import io.github.viniciussambinello.stags.testkit.InMemorySelectionRepository;
import io.github.viniciussambinello.stags.testkit.StubPermissionOracle;

final class SelectorServiceTest {

    private final UUID playerId = UUID.randomUUID();

    private SelectorService build(
            final boolean grantVip, final Selection activeTag) throws Exception {
        final InMemoryCosmeticRepository cosmeticRepository = new InMemoryCosmeticRepository();
        cosmeticRepository.insert(new Cosmetic(
                CosmeticKind.TAG, new CosmeticId("vip"), Prefix.parse("<gold>[VIP]</gold>"),
                new PermissionNode("stags.tag.vip"), new Weight(100))).get();
        cosmeticRepository.insert(new Cosmetic(
                CosmeticKind.TAG, new CosmeticId("member"), Prefix.parse("<gray>[Member]</gray>"),
                PermissionNode.NONE, new Weight(10))).get();
        final CatalogueService catalogueService = new CatalogueService(cosmeticRepository);
        catalogueService.loadInitial().get();

        final InMemorySelectionRepository selectionRepository = new InMemorySelectionRepository();
        selectionRepository.put(new PlayerCosmetics(playerId, activeTag, Selection.UNSET));
        final PlayerCosmeticService playerCosmeticService = new PlayerCosmeticService(selectionRepository);
        playerCosmeticService.load(playerId).get();

        final StubPermissionOracle permissions = new StubPermissionOracle();
        if (grantVip) {
            permissions.grant(playerId, "stags.tag.vip");
        }
        return new SelectorService(catalogueService, playerCosmeticService, permissions);
    }

    @Test
    void listsAllEntriesWithOwnershipAndActiveMarking() throws Exception {
        final SelectorService service = build(true, new Selection.Active(new CosmeticId("vip")));
        final List<SelectorService.Entry> entries = service.entries(playerId, CosmeticKind.TAG, false);

        assertEquals(2, entries.size());
        assertEquals("vip", entries.get(0).cosmetic().id().value());
        assertTrue(entries.get(0).owned());
        assertTrue(entries.get(0).active());
        assertTrue(entries.get(1).owned());
        assertFalse(entries.get(1).active());
    }

    @Test
    void hidingLockedEntriesFiltersUnownedCosmetics() throws Exception {
        final SelectorService service = build(false, Selection.UNSET);
        final List<SelectorService.Entry> entries = service.entries(playerId, CosmeticKind.TAG, true);

        assertEquals(1, entries.size());
        assertEquals("member", entries.get(0).cosmetic().id().value());
    }

    @Test
    void showingLockedEntriesIncludesUnownedCosmetics() throws Exception {
        final SelectorService service = build(false, Selection.UNSET);
        final List<SelectorService.Entry> entries = service.entries(playerId, CosmeticKind.TAG, false);

        assertEquals(2, entries.size());
        assertFalse(entries.get(0).owned());
    }

    @Test
    void hasActiveSelectionReflectsCurrentState() throws Exception {
        final SelectorService activeService = build(true, new Selection.Active(new CosmeticId("vip")));
        assertTrue(activeService.hasActiveSelection(playerId, CosmeticKind.TAG));

        final SelectorService clearedService = build(true, Selection.CLEARED);
        assertFalse(clearedService.hasActiveSelection(playerId, CosmeticKind.TAG));
    }
}
