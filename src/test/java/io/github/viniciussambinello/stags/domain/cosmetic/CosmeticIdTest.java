package io.github.viniciussambinello.stags.domain.cosmetic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class CosmeticIdTest {

    @Test
    void acceptsAllowedAlphabet() {
        final CosmeticId id = new CosmeticId("vip-2_gold");
        assertEquals("vip-2_gold", id.value());
    }

    @Test
    void normalizesCaseForComparisonPurposes() {
        final CosmeticId lower = new CosmeticId("vip");
        final CosmeticId upper = new CosmeticId("VIP");
        assertEquals(lower, upper);
    }

    @Test
    void rejectsWhitespace() {
        assertThrows(IllegalArgumentException.class, () -> new CosmeticId("vip gold"));
    }

    @Test
    void rejectsPeriod() {
        assertThrows(IllegalArgumentException.class, () -> new CosmeticId("vip.gold"));
    }

    @Test
    void rejectsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new CosmeticId(""));
    }

    @Test
    void rejectsOverMaxLength() {
        final String tooLong = "a".repeat(33);
        assertThrows(IllegalArgumentException.class, () -> new CosmeticId(tooLong));
    }

    @Test
    void isValidReflectsConstructionOutcome() {
        assertTrue(CosmeticId.isValid("vip"));
        assertFalse(CosmeticId.isValid("vip gold"));
        assertFalse(CosmeticId.isValid(null));
    }
}
