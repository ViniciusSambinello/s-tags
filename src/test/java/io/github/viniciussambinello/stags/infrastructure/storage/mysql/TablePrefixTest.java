package io.github.viniciussambinello.stags.infrastructure.storage.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class TablePrefixTest {

    @Test
    void acceptsAlphanumericAndUnderscore() {
        assertEquals("stags_", TablePrefix.validate("stags_"));
    }

    @Test
    void acceptsEmptyPrefix() {
        assertEquals("", TablePrefix.validate(""));
    }

    @Test
    void rejectsInjectionAttempt() {
        assertThrows(IllegalArgumentException.class, () -> TablePrefix.validate("stags_; DROP TABLE users; --"));
    }

    @Test
    void rejectsSpaces() {
        assertThrows(IllegalArgumentException.class, () -> TablePrefix.validate("my prefix"));
    }
}
