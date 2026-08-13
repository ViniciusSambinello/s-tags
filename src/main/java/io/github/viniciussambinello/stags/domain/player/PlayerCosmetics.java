package io.github.viniciussambinello.stags.domain.player;

import java.util.Objects;
import java.util.UUID;

import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;

public record PlayerCosmetics(UUID playerId, Selection tagSelection, Selection titleSelection) {

    public PlayerCosmetics {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(tagSelection, "tagSelection");
        Objects.requireNonNull(titleSelection, "titleSelection");
    }

    public static PlayerCosmetics unset(final UUID playerId) {
        return new PlayerCosmetics(playerId, Selection.UNSET, Selection.UNSET);
    }

    public Selection selection(final CosmeticKind kind) {
        return switch (kind) {
            case TAG -> tagSelection;
            case TITLE -> titleSelection;
        };
    }

    public PlayerCosmetics withSelection(final CosmeticKind kind, final Selection selection) {
        Objects.requireNonNull(selection, "selection");
        return switch (kind) {
            case TAG -> new PlayerCosmetics(playerId, selection, titleSelection);
            case TITLE -> new PlayerCosmetics(playerId, tagSelection, selection);
        };
    }
}
