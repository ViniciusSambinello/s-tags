package io.github.viniciussambinello.stags.testkit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.viniciussambinello.stags.application.port.CosmeticRenderer;

public final class RecordingCosmeticRenderer implements CosmeticRenderer {

    private final List<UUID> refreshed = new ArrayList<>();

    @Override
    public void refresh(final UUID playerId) {
        refreshed.add(playerId);
    }

    public List<UUID> refreshed() {
        return List.copyOf(refreshed);
    }
}
