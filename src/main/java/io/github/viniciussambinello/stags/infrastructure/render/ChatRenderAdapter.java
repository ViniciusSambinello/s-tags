package io.github.viniciussambinello.stags.infrastructure.render;

import java.util.Optional;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import io.papermc.paper.event.player.AsyncChatEvent;

import io.github.viniciussambinello.stags.application.service.ActiveCosmeticResolver;
import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.infrastructure.config.ConfigService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class ChatRenderAdapter implements Listener {

    private static final MiniMessage RAW_MESSAGE_PARSER = MiniMessage.miniMessage();

    private final ConfigService configService;
    private final ActiveCosmeticResolver activeCosmeticResolver;

    public ChatRenderAdapter(final ConfigService configService, final ActiveCosmeticResolver activeCosmeticResolver) {
        this.configService = configService;
        this.activeCosmeticResolver = activeCosmeticResolver;
    }

    @EventHandler
    public void onChat(final AsyncChatEvent event) {
        if (!configService.config().render().chat().enabled()) {
            return;
        }

        final Player sender = event.getPlayer();
        final Optional<Cosmetic> activeTag = activeCosmeticResolver.activeCosmetic(sender.getUniqueId(), CosmeticKind.TAG);
        final Component tagPrefix = activeTag
                .map(tag -> tag.prefix().rendered().append(Component.space()))
                .orElse(Component.empty());

        final String format = configService.config().render().chat().format();
        final String formattingPermission = configService.config().render().chat().formattingPermission();
        final boolean canFormat = sender.hasPermission(formattingPermission);

        event.renderer((source, sourceDisplayName, message, viewer) -> {
            final Component body = canFormat
                    ? RAW_MESSAGE_PARSER.deserialize(PlainTextComponentSerializer.plainText().serialize(message))
                    : message;
            return ChatFormat.render(format, tagPrefix, sourceDisplayName, body);
        });
    }
}
