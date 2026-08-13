package io.github.viniciussambinello.stags.infrastructure.command;

import org.bukkit.command.CommandSender;

import io.github.viniciussambinello.stags.infrastructure.config.ConfigService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

final class CommandMessages {

    static void sendPlayerOnly(final ConfigService configService, final CommandSender sender) {
        send(configService, sender, "general.player-only");
    }

    static void sendUsage(final ConfigService configService, final CommandSender sender, final String usage) {
        send(configService, sender, "general.usage", Placeholder.component("usage", net.kyori.adventure.text.Component.text(usage)));
    }

    static void sendNoPermission(final ConfigService configService, final CommandSender sender) {
        send(configService, sender, "general.no-permission");
    }

    static void send(final ConfigService configService, final CommandSender sender, final String key, final TagResolver... placeholders) {
        configService.messages().render(key, placeholders).ifPresent(sender::sendMessage);
    }

    private CommandMessages() {
    }
}
