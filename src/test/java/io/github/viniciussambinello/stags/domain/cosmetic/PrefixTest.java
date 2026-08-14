package io.github.viniciussambinello.stags.domain.cosmetic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

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

    @Test
    void convertsLegacyAmpersandColorCodesToMiniMessage() {
        final Prefix prefix = Prefix.parse("&c[VIP]");
        assertEquals(NamedTextColor.RED, prefix.rendered().color());
        assertEquals("[VIP]", PlainTextComponentSerializer.plainText().serialize(prefix.rendered()));
        assertTrue(prefix.raw().contains("<red>"));
    }

    @Test
    void leavesMiniMessageTagsUnaffectedByLegacyNormalization() {
        final Prefix prefix = Prefix.parse("<gold>[VIP]</gold>");
        assertEquals("<gold>[VIP]</gold>", prefix.raw());
    }

    @Test
    void leavesPlainTextWithNoCodesUnaffected() {
        final Prefix prefix = Prefix.parse("VIP");
        assertEquals("VIP", prefix.raw());
    }

    @Test
    void aLegacyConvertedPrefixSurvivesReloadFromStorage() {
        final Prefix authored = Prefix.parse("&c[VIP]");
        final Prefix reloaded = Prefix.parseStored(authored.raw());
        assertEquals(NamedTextColor.RED, reloaded.rendered().color());
        assertEquals("[VIP]", PlainTextComponentSerializer.plainText().serialize(reloaded.rendered()));
    }

    @Test
    void aLegacyConvertedPrefixSurvivesBeingStrictlyReparsed() {
        final Prefix authored = Prefix.parse("&c[Owner]");
        final Prefix reconfirmed = Prefix.parse(authored.raw());
        assertEquals(NamedTextColor.RED, reconfirmed.rendered().color());
        assertEquals("[Owner]", PlainTextComponentSerializer.plainText().serialize(reconfirmed.rendered()));
    }
}
