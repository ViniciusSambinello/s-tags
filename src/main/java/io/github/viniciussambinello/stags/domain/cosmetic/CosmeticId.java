package io.github.viniciussambinello.stags.domain.cosmetic;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record CosmeticId(String value) implements Comparable<CosmeticId> {

    private static final Pattern ALLOWED_CHARACTERS = Pattern.compile("[a-z0-9_-]+");
    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 32;

    public CosmeticId {
        Objects.requireNonNull(value, "value");
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Identifier must be between " + MIN_LENGTH + " and " + MAX_LENGTH
                            + " characters, was " + value.length() + " ('" + value + "')");
        }
        if (!ALLOWED_CHARACTERS.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Identifier '" + value + "' must only contain lowercase letters, digits, hyphen and underscore");
        }
    }

    public static boolean isValid(final String candidate) {
        if (candidate == null) {
            return false;
        }
        try {
            new CosmeticId(candidate);
            return true;
        } catch (final IllegalArgumentException exception) {
            return false;
        }
    }

    @Override
    public int compareTo(final CosmeticId other) {
        return value.compareTo(other.value);
    }
}
