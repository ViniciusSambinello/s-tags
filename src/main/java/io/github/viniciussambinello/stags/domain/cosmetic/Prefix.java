package io.github.viniciussambinello.stags.domain.cosmetic;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public record Prefix(String raw, Component rendered, boolean placeholderBearing) {

    private static final int MAX_LENGTH = 128;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%[^%\\s]+%");
    private static final Pattern LEGACY_CODE_PATTERN = Pattern.compile("&[0-9a-fk-orA-FK-OR]");
    private static final Pattern SIMPLE_TAG_PATTERN = Pattern.compile("<(/?)([a-zA-Z_]+)>");
    private static final MiniMessage STRICT_MINI_MESSAGE = MiniMessage.builder().strict(true).build();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

    public Prefix {
        Objects.requireNonNull(raw, "raw");
        Objects.requireNonNull(rendered, "rendered");
        if (raw.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Prefix must be at most " + MAX_LENGTH + " characters, was " + raw.length());
        }
    }

    public static Prefix parse(final String raw) {
        Objects.requireNonNull(raw, "raw");
        if (isLegacyOnly(raw)) {
            final Component legacyRendered = LEGACY_AMPERSAND.deserialize(raw);
            final String normalized = closeDanglingTags(MINI_MESSAGE.serialize(legacyRendered));
            return new Prefix(normalized, legacyRendered, PLACEHOLDER_PATTERN.matcher(normalized).find());
        }
        final Component rendered;
        try {
            rendered = STRICT_MINI_MESSAGE.deserialize(raw);
        } catch (final ParsingException exception) {
            throw new PrefixParseException("Prefix '" + raw + "' is not valid MiniMessage: " + exception.getMessage(), exception);
        }
        return new Prefix(raw, rendered, PLACEHOLDER_PATTERN.matcher(raw).find());
    }

    public static Prefix parseStored(final String raw) {
        Objects.requireNonNull(raw, "raw");
        final Component rendered = MINI_MESSAGE.deserialize(raw);
        return new Prefix(raw, rendered, PLACEHOLDER_PATTERN.matcher(raw).find());
    }

    private static boolean isLegacyOnly(final String raw) {
        return raw.indexOf('<') < 0 && LEGACY_CODE_PATTERN.matcher(raw).find();
    }

    private static String closeDanglingTags(final String miniMessage) {
        final Deque<String> openTags = new ArrayDeque<>();
        final Matcher matcher = SIMPLE_TAG_PATTERN.matcher(miniMessage);
        while (matcher.find()) {
            if ("/".equals(matcher.group(1))) {
                if (!openTags.isEmpty()) {
                    openTags.removeLast();
                }
            } else {
                openTags.addLast(matcher.group(2));
            }
        }
        if (openTags.isEmpty()) {
            return miniMessage;
        }
        final StringBuilder closed = new StringBuilder(miniMessage);
        while (!openTags.isEmpty()) {
            closed.append("</").append(openTags.removeLast()).append('>');
        }
        return closed.toString();
    }
}
