package io.github.viniciussambinello.stags.infrastructure.placeholder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.github.viniciussambinello.stags.application.service.ActiveCosmeticResolver;
import io.github.viniciussambinello.stags.application.service.CatalogueService;
import io.github.viniciussambinello.stags.application.service.PlayerCosmeticService;
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

final class StagsExpansionTest {

    @Test
    void resolvesPublishedPlaceholdersForAnOnlinePlayer() throws Exception {
        final InMemoryCosmeticRepository cosmeticRepository = new InMemoryCosmeticRepository();
        cosmeticRepository.insert(new Cosmetic(
                CosmeticKind.TAG, new CosmeticId("vip"), Prefix.parse("<gold>[VIP]</gold>"),
                new PermissionNode("stags.tag.vip"), new Weight(100))).get();
        final CatalogueService catalogueService = new CatalogueService(cosmeticRepository);
        catalogueService.loadInitial().get();

        final UUID playerId = UUID.randomUUID();
        final InMemorySelectionRepository selectionRepository = new InMemorySelectionRepository();
        selectionRepository.put(new PlayerCosmetics(playerId, new Selection.Active(new CosmeticId("vip")), Selection.UNSET));
        final PlayerCosmeticService playerCosmeticService = new PlayerCosmeticService(selectionRepository);
        playerCosmeticService.load(playerId).get();

        final StubPermissionOracle permissions = new StubPermissionOracle().grant(playerId, "stags.tag.vip");
        final ActiveCosmeticResolver resolver = new ActiveCosmeticResolver(catalogueService, playerCosmeticService, permissions);
        final StagsExpansion expansion = new StagsExpansion("0.1.0", resolver, catalogueService, permissions);

        final Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(playerId);
        Mockito.when(player.isOnline()).thenReturn(true);

        assertEquals("vip", expansion.onRequest(player, "tag"));
        assertEquals("[VIP]", expansion.onRequest(player, "tag_prefix"));
        assertEquals("100", expansion.onRequest(player, "tag_weight"));
        assertEquals("", expansion.onRequest(player, "title"));
        assertEquals("1", expansion.onRequest(player, "tag_count"));
        assertEquals("0", expansion.onRequest(player, "title_count"));
    }

    @Test
    void unrecognizedPlaceholderResolvesToNull() {
        final CatalogueService catalogueService = new CatalogueService(new InMemoryCosmeticRepository());
        final PlayerCosmeticService playerCosmeticService = new PlayerCosmeticService(new InMemorySelectionRepository());
        final StubPermissionOracle permissions = new StubPermissionOracle();
        final ActiveCosmeticResolver resolver = new ActiveCosmeticResolver(catalogueService, playerCosmeticService, permissions);
        final StagsExpansion expansion = new StagsExpansion("0.1.0", resolver, catalogueService, permissions);

        final Player player = Mockito.mock(Player.class);
        Mockito.when(player.isOnline()).thenReturn(true);

        assertNull(expansion.onRequest(player, "not_a_real_placeholder"));
    }

    @Test
    void offlinePlayerResolvesToEmptyForKnownPlaceholders() {
        final CatalogueService catalogueService = new CatalogueService(new InMemoryCosmeticRepository());
        final PlayerCosmeticService playerCosmeticService = new PlayerCosmeticService(new InMemorySelectionRepository());
        final StubPermissionOracle permissions = new StubPermissionOracle();
        final ActiveCosmeticResolver resolver = new ActiveCosmeticResolver(catalogueService, playerCosmeticService, permissions);
        final StagsExpansion expansion = new StagsExpansion("0.1.0", resolver, catalogueService, permissions);

        final OfflinePlayer offlinePlayer = Mockito.mock(OfflinePlayer.class);

        assertEquals("", expansion.onRequest(offlinePlayer, "tag"));
        assertNull(expansion.onRequest(offlinePlayer, "not_a_real_placeholder"));
    }
}
