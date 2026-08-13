package io.github.viniciussambinello.stags.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

final class MessageCatalogTest {

    private static YamlConfiguration defaultDoc() {
        final YamlConfiguration document = new YamlConfiguration();
        document.set("prefix", "<gray>[Prefix]</gray> ");
        document.set("unprefixed-messages", java.util.List.of());
        document.set("messages.general.hello", "<green>Hello <player>.</green>");
        return document;
    }

    @Test
    void emptyMessageIsSuppressed() {
        final YamlConfiguration live = defaultDoc();
        live.set("messages.general.hello", "");
        final MessageCatalog.LoadResult result = MessageCatalog.load(live, defaultDoc());

        final Optional<Component> rendered = result.catalog().render("general.hello");
        assertTrue(rendered.isEmpty());
    }

    @Test
    void unknownPlaceholderIsLeftLiteralRatherThanErroring() {
        final YamlConfiguration live = defaultDoc();
        live.set("messages.general.hello", "<green>Hello <not_a_real_placeholder>.</green>");
        final MessageCatalog.LoadResult result = MessageCatalog.load(live, defaultDoc());

        final Optional<Component> rendered = result.catalog().render("general.hello");
        assertTrue(rendered.isPresent());
        final String plain = PlainTextComponentSerializer.plainText().serialize(rendered.get());
        assertTrue(plain.contains("<not_a_real_placeholder>"));
    }

    @Test
    void malformedMessageFallsBackToShippedDefaultAndWarns() {
        final YamlConfiguration live = defaultDoc();
        live.set("messages.general.hello", "<gold><bold>Unclosed");
        final MessageCatalog.LoadResult result = MessageCatalog.load(live, defaultDoc());

        assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("general.hello")));
        final Optional<Component> rendered = result.catalog().render("general.hello", Placeholder.component("player", Component.text("Alex")));
        assertTrue(rendered.isPresent());
        final String plain = PlainTextComponentSerializer.plainText().serialize(rendered.get());
        assertTrue(plain.contains("Hello Alex"));
    }

    @Test
    void placeholderIsResolvedAndPrefixIsPrepended() {
        final YamlConfiguration live = defaultDoc();
        final MessageCatalog.LoadResult result = MessageCatalog.load(live, defaultDoc());

        final Optional<Component> rendered = result.catalog().render(
                "general.hello", Placeholder.component("player", Component.text("Alex")));
        assertTrue(rendered.isPresent());
        final String plain = PlainTextComponentSerializer.plainText().serialize(rendered.get());
        assertEquals("[Prefix] Hello Alex.", plain);
    }

    @Test
    void unprefixedKeySkipsThePrefix() {
        final YamlConfiguration live = defaultDoc();
        live.set("unprefixed-messages", java.util.List.of("general.hello"));
        final MessageCatalog.LoadResult result = MessageCatalog.load(live, defaultDoc());

        final Optional<Component> rendered = result.catalog().render(
                "general.hello", Placeholder.component("player", Component.text("Alex")));
        final String plain = PlainTextComponentSerializer.plainText().serialize(rendered.orElseThrow());
        assertEquals("Hello Alex.", plain);
    }
}
