package io.github.viniciussambinello.stags.infrastructure.config;

import java.util.List;
import java.util.Objects;

public record MenuLayoutConfig(
        int size,
        List<Integer> contentSlots,
        String entryMaterial,
        String lockedMaterial,
        String fillerMaterial,
        int previousPageSlot,
        int nextPageSlot,
        int clearSelectionSlot) {

    public MenuLayoutConfig {
        Objects.requireNonNull(contentSlots, "contentSlots");
        contentSlots = List.copyOf(contentSlots);
    }
}
