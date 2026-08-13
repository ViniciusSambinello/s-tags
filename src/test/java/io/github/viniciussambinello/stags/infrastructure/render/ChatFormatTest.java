package io.github.viniciussambinello.stags.infrastructure.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

final class ChatFormatTest {

    private static final String FORMAT = "<tag_prefix><white><player></white><gray>:</gray> <message>";

    @Test
    void rendersWithTagPrefix() {
        final Component result = ChatFormat.render(
                FORMAT, Component.text("[VIP] "), Component.text("Alex"), Component.text("hello"));
        assertEquals("[VIP] Alex: hello", PlainTextComponentSerializer.plainText().serialize(result));
    }

    @Test
    void emptyTagPrefixLeavesNoLeadingGap() {
        final Component result = ChatFormat.render(
                FORMAT, Component.empty(), Component.text("Alex"), Component.text("hello"));
        assertEquals("Alex: hello", PlainTextComponentSerializer.plainText().serialize(result));
    }
}
