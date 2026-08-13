package io.github.viniciussambinello.stags.infrastructure.render;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

final class ChatFormat {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    static Component render(final String format, final Component tagPrefix, final Component playerName, final Component body) {
        return MINI_MESSAGE.deserialize(
                format,
                Placeholder.component("tag_prefix", tagPrefix),
                Placeholder.component("player", playerName),
                Placeholder.component("message", body));
    }

    private ChatFormat() {
    }
}
