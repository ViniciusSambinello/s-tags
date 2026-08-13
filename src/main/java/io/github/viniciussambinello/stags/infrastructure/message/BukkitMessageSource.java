package io.github.viniciussambinello.stags.infrastructure.message;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.entity.Player;

import io.github.viniciussambinello.stags.application.port.MessageSource;
import io.github.viniciussambinello.stags.infrastructure.config.ConfigService;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public final class BukkitMessageSource implements MessageSource {

    private final Server server;
    private final ConfigService configService;

    public BukkitMessageSource(final Server server, final ConfigService configService) {
        this.server = server;
        this.configService = configService;
    }

    @Override
    public void send(final UUID playerId, final String messageKey, final TagResolver... placeholders) {
        final Player player = server.getPlayer(playerId);
        if (player == null) {
            return;
        }
        configService.messages().render(messageKey, placeholders).ifPresent(player::sendMessage);
    }
}
