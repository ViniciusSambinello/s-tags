package io.github.viniciussambinello.stags.domain.cosmetic;

import java.util.Objects;
import java.util.regex.Pattern;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;

public record Prefix(String raw, Component rendered, boolean placeholderBearing) {

    private static final int MAX_LENGTH = 128;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%[^%\\s]+%");
    private static final MiniMessage STRICT_MINI_MESSAGE = MiniMessage.builder().strict(true).build();

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
        final Component rendered;
        try {
            rendered = STRICT_MINI_MESSAGE.deserialize(raw);
        } catch (final ParsingException exception) {
            throw new PrefixParseException("Prefix '" + raw + "' is not valid MiniMessage: " + exception.getMessage(), exception);
        }
        return new Prefix(raw, rendered, PLACEHOLDER_PATTERN.matcher(raw).find());
    }
}
