package io.github.viniciussambinello.stags.infrastructure.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import io.github.viniciussambinello.stags.infrastructure.placeholder.NoopPlaceholderResolver;
import io.github.viniciussambinello.stags.testkit.InMemoryCosmeticRepository;
import io.github.viniciussambinello.stags.testkit.InMemorySelectionRepository;
import io.github.viniciussambinello.stags.testkit.StubPermissionOracle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

final class ChatRenderAdapterTest {

    private Player mockPlayer(final UUID id, final boolean canFormat) {
        final Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(id);
        Mockito.when(player.hasPermission("stags.chat.format")).thenReturn(canFormat);
        return player;
    }

    private ChatRenderAdapter newAdapter(final Path dir, final UUID playerId, final boolean ownsTag) throws Exception {
        final InMemoryCosmeticRepository cosmeticRepository = new InMemoryCosmeticRepository();
        cosmeticRepository.insert(new Cosmetic(
                CosmeticKind.TAG, new CosmeticId("vip"), Prefix.parse("<gold>[VIP]</gold>"),
                new PermissionNode("stags.tag.vip"), new Weight(100))).get();
        final CatalogueService catalogueService = new CatalogueService(cosmeticRepository);
        catalogueService.loadInitial().get();

        final InMemorySelectionRepository selectionRepository = new InMemorySelectionRepository();
        if (ownsTag) {
            selectionRepository.put(new PlayerCosmetics(playerId, new Selection.Active(new CosmeticId("vip")), Selection.UNSET));
        } else {
            selectionRepository.put(new PlayerCosmetics(playerId, Selection.CLEARED, Selection.UNSET));
        }
        final PlayerCosmeticService playerCosmeticService = new PlayerCosmeticService(selectionRepository);
        playerCosmeticService.load(playerId).get();

        final StubPermissionOracle permissions = ownsTag
                ? new StubPermissionOracle().grant(playerId, "stags.tag.vip")
                : new StubPermissionOracle();
        final ActiveCosmeticResolver activeCosmeticResolver =
                new ActiveCosmeticResolver(catalogueService, playerCosmeticService, permissions);
        final ConfigService configService = ConfigService.initial(dir, Logger.getLogger("test"));

        return new ChatRenderAdapter(configService, activeCosmeticResolver, new NoopPlaceholderResolver());
    }

    @Test
    void markupInTheMessageBodyIsRenderedLiterallyWithoutTheFormattingPermission(@TempDir final Path dir) throws Exception {
        final UUID playerId = UUID.randomUUID();
        final ChatRenderAdapter adapter = newAdapter(dir, playerId, false);
        final Player sender = mockPlayer(playerId, false);

        final Component rendered = adapter.render(sender, Component.text("Steve"), Component.text("<red>test</red>"));

        final String plain = PlainTextComponentSerializer.plainText().serialize(rendered);
        assertTrue(plain.contains("<red>test</red>"));
    }

    @Test
    void markupInTheMessageBodyIsParsedWithTheFormattingPermission(@TempDir final Path dir) throws Exception {
        final UUID playerId = UUID.randomUUID();
        final ChatRenderAdapter adapter = newAdapter(dir, playerId, true);
        final Player sender = mockPlayer(playerId, true);

        final Component rendered = adapter.render(sender, Component.text("Steve"), Component.text("<red>test</red>"));

        final String plain = PlainTextComponentSerializer.plainText().serialize(rendered);
        assertFalse(plain.contains("<red>"));
        assertTrue(plain.contains("test"));
    }

    @Test
    void activeTagPrefixIsIncludedInTheRenderedMessage(@TempDir final Path dir) throws Exception {
        final UUID playerId = UUID.randomUUID();
        final ChatRenderAdapter adapter = newAdapter(dir, playerId, true);
        final Player sender = mockPlayer(playerId, true);

        final Component rendered = adapter.render(sender, Component.text("Steve"), Component.text("hello"));

        final String plain = PlainTextComponentSerializer.plainText().serialize(rendered);
        assertTrue(plain.contains("[VIP]"));
    }

    @Test
    void noActiveTagLeavesNoPrefixInTheRenderedMessage(@TempDir final Path dir) throws Exception {
        final UUID playerId = UUID.randomUUID();
        final ChatRenderAdapter adapter = newAdapter(dir, playerId, false);
        final Player sender = mockPlayer(playerId, true);

        final Component rendered = adapter.render(sender, Component.text("Steve"), Component.text("hello"));

        final String plain = PlainTextComponentSerializer.plainText().serialize(rendered);
        assertFalse(plain.contains("[VIP]"));
    }
}
