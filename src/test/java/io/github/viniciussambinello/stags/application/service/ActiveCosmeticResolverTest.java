package io.github.viniciussambinello.stags.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

final class ActiveCosmeticResolverTest {

    private final UUID playerId = UUID.randomUUID();

    @Test
    void resolvesTheActiveOwnedTag() throws Exception {
        final InMemoryCosmeticRepository cosmeticRepository = new InMemoryCosmeticRepository();
        cosmeticRepository.insert(new Cosmetic(
                CosmeticKind.TAG, new CosmeticId("vip"), Prefix.parse("<gold>[VIP]</gold>"),
                new PermissionNode("stags.tag.vip"), new Weight(100))).get();
        final CatalogueService catalogueService = new CatalogueService(cosmeticRepository);
        catalogueService.loadInitial().get();

        final InMemorySelectionRepository selectionRepository = new InMemorySelectionRepository();
        selectionRepository.put(new PlayerCosmetics(playerId, new Selection.Active(new CosmeticId("vip")), Selection.UNSET));
        final PlayerCosmeticService playerCosmeticService = new PlayerCosmeticService(selectionRepository);
        playerCosmeticService.load(playerId).get();

        final StubPermissionOracle permissions = new StubPermissionOracle().grant(playerId, "stags.tag.vip");
        final ActiveCosmeticResolver resolver = new ActiveCosmeticResolver(catalogueService, playerCosmeticService, permissions);

        final Cosmetic resolved = resolver.activeCosmetic(playerId, CosmeticKind.TAG).orElseThrow();
        assertEquals("vip", resolved.id().value());
    }

    @Test
    void returnsEmptyWhenPlayerNotYetLoaded() {
        final CatalogueService catalogueService = new CatalogueService(new InMemoryCosmeticRepository());
        final PlayerCosmeticService playerCosmeticService = new PlayerCosmeticService(new InMemorySelectionRepository());
        final ActiveCosmeticResolver resolver =
                new ActiveCosmeticResolver(catalogueService, playerCosmeticService, new StubPermissionOracle());

        assertTrue(resolver.activeCosmetic(playerId, CosmeticKind.TAG).isEmpty());
    }
}
