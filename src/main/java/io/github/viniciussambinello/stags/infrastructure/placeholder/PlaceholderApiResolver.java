package io.github.viniciussambinello.stags.infrastructure.placeholder;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.entity.Player;

import me.clip.placeholderapi.PlaceholderAPI;

import io.github.viniciussambinello.stags.application.port.PlaceholderResolver;
import io.github.viniciussambinello.stags.domain.cosmetic.Prefix;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class PlaceholderApiResolver implements PlaceholderResolver {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Server server;

    public PlaceholderApiResolver(final Server server) {
        this.server = server;
    }

    @Override
    public Component resolve(final Prefix prefix, final UUID wearingPlayerId) {
        if (!prefix.placeholderBearing()) {
            return prefix.rendered();
        }
        final Player wearingPlayer = server.getPlayer(wearingPlayerId);
        if (wearingPlayer == null) {
            return prefix.rendered();
        }
        final String resolvedRaw = PlaceholderAPI.setPlaceholders(wearingPlayer, prefix.raw());
        return MINI_MESSAGE.deserialize(resolvedRaw);
    }
}
