package io.github.viniciussambinello.stags.infrastructure.placeholder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import me.clip.placeholderapi.PlaceholderAPI;

import io.github.viniciussambinello.stags.domain.cosmetic.Prefix;
import net.kyori.adventure.text.minimessage.MiniMessage;

final class PlaceholderApiResolverTest {

    @Test
    void aStaticPrefixNeverInvokesPlaceholderApi() {
        final Server server = Mockito.mock(Server.class);
        final PlaceholderApiResolver resolver = new PlaceholderApiResolver(server);
        final Prefix prefix = Prefix.parse("<gold>[VIP]</gold>");

        try (MockedStatic<PlaceholderAPI> mocked = Mockito.mockStatic(PlaceholderAPI.class)) {
            final var result = resolver.resolve(prefix, UUID.randomUUID());

            assertEquals(prefix.rendered(), result);
            mocked.verifyNoInteractions();
        }
    }

    @Test
    void aPlaceholderBearingPrefixIsResolvedForAnOnlinePlayerInASinglePass() {
        final UUID playerId = UUID.randomUUID();
        final Player player = Mockito.mock(Player.class);
        final Server server = Mockito.mock(Server.class);
        Mockito.when(server.getPlayer(playerId)).thenReturn(player);
        final PlaceholderApiResolver resolver = new PlaceholderApiResolver(server);
        final Prefix prefix = Prefix.parse("<gold>%player_name%</gold>");

        try (MockedStatic<PlaceholderAPI> mocked = Mockito.mockStatic(PlaceholderAPI.class)) {
            mocked.when(() -> PlaceholderAPI.setPlaceholders(player, prefix.raw())).thenReturn("<gold>Steve</gold>");

            final var result = resolver.resolve(prefix, playerId);

            assertEquals(MiniMessage.miniMessage().deserialize("<gold>Steve</gold>"), result);
            mocked.verify(() -> PlaceholderAPI.setPlaceholders(player, prefix.raw()), Mockito.times(1));
        }
    }

    @Test
    void aPlaceholderBearingPrefixForAnOfflinePlayerRendersLiterallyWithoutCallingPlaceholderApi() {
        final UUID playerId = UUID.randomUUID();
        final Server server = Mockito.mock(Server.class);
        Mockito.when(server.getPlayer(playerId)).thenReturn(null);
        final PlaceholderApiResolver resolver = new PlaceholderApiResolver(server);
        final Prefix prefix = Prefix.parse("<gold>%player_name%</gold>");

        try (MockedStatic<PlaceholderAPI> mocked = Mockito.mockStatic(PlaceholderAPI.class)) {
            final var result = resolver.resolve(prefix, playerId);

            assertEquals(prefix.rendered(), result);
            mocked.verifyNoInteractions();
        }
    }
}
