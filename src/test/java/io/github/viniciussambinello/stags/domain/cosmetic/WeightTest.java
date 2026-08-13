package io.github.viniciussambinello.stags.domain.cosmetic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class WeightTest {

    @Test
    void acceptsZero() {
        assertEquals(0, new Weight(0).value());
    }

    @Test
    void acceptsPositiveValues() {
        assertEquals(100, new Weight(100).value());
    }

    @Test
    void rejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> new Weight(-1));
    }
}
