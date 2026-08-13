package io.github.viniciussambinello.stags.domain.cosmetic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PrefixTest {

    @Test
    void parsesValidMiniMessage() {
        final Prefix prefix = Prefix.parse("<gold>[VIP]</gold>");
        assertFalse(prefix.placeholderBearing());
    }

    @Test
    void rejectsUnclosedTag() {
        assertThrows(PrefixParseException.class, () -> Prefix.parse("<gold>[VIP]"));
    }

    @Test
    void detectsPlaceholderBearingPrefix() {
        final Prefix prefix = Prefix.parse("<gold>%player_name%</gold>");
        assertTrue(prefix.placeholderBearing());
    }
}
