package io.github.viniciussambinello.stags.infrastructure.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
import io.github.viniciussambinello.stags.infrastructure.config.ConfigService;
import io.github.viniciussambinello.stags.testkit.InMemoryCosmeticRepository;
import io.github.viniciussambinello.stags.testkit.InMemorySelectionRepository;
import io.github.viniciussambinello.stags.testkit.StubPermissionOracle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

final class TabListRenderAdapterTest {

    @Test
    void setsPlayerListNameWithPrefixAndWeightOrder(@TempDir final Path dir) throws Exception {
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

        final ActiveCosmeticResolver resolver = new ActiveCosmeticResolver(
                catalogueService, playerCosmeticService, new StubPermissionOracle().grant(playerId, "stags.tag.vip"));
        final ConfigService configService = ConfigService.initial(dir, Logger.getLogger("test"));
        final TabListRenderAdapter adapter = new TabListRenderAdapter(configService, resolver);

        final Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(playerId);
        Mockito.when(player.getName()).thenReturn("Steve");

        adapter.refresh(player);

        final org.mockito.ArgumentCaptor<Component> nameCaptor = org.mockito.ArgumentCaptor.forClass(Component.class);
        Mockito.verify(player).playerListName(nameCaptor.capture());
        assertEquals("[VIP] Steve", PlainTextComponentSerializer.plainText().serialize(nameCaptor.getValue()));
        Mockito.verify(player).setPlayerListOrder(100);
    }

    @Test
    void teardownClearsPlayerListName(@TempDir final Path dir) throws Exception {
        final CatalogueService catalogueService = new CatalogueService(new InMemoryCosmeticRepository());
        catalogueService.loadInitial().get();
        final PlayerCosmeticService playerCosmeticService = new PlayerCosmeticService(new InMemorySelectionRepository());
        final ActiveCosmeticResolver resolver =
                new ActiveCosmeticResolver(catalogueService, playerCosmeticService, new StubPermissionOracle());
        final ConfigService configService = ConfigService.initial(dir, Logger.getLogger("test"));
        final TabListRenderAdapter adapter = new TabListRenderAdapter(configService, resolver);

        final Player player = Mockito.mock(Player.class);
        adapter.teardown(player);

        Mockito.verify(player).playerListName(any());
        Mockito.verify(player).setPlayerListOrder(0);
    }
}
