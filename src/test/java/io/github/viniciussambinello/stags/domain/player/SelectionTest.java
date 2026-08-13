package io.github.viniciussambinello.stags.domain.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;

final class SelectionTest {

    @Test
    void exhaustiveSwitchCoversEveryVariant() {
        assertEquals("unset", describe(Selection.UNSET));
        assertEquals("cleared", describe(Selection.CLEARED));
        assertEquals("active:vip", describe(new Selection.Active(new CosmeticId("vip"))));
    }

    private String describe(final Selection selection) {
        return switch (selection) {
            case Selection.Unset unset -> "unset";
            case Selection.Cleared cleared -> "cleared";
            case Selection.Active active -> "active:" + active.cosmeticId().value();
        };
    }
}
