package io.github.viniciussambinello.stags.application.port;

import java.util.UUID;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public interface MessageSource {

    void send(UUID playerId, String messageKey, TagResolver... placeholders);
}
