package io.github.viniciussambinello.stags.domain.player;

import java.util.Objects;

import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;

public sealed interface Selection {

    Selection UNSET = new Unset();
    Selection CLEARED = new Cleared();

    record Unset() implements Selection {
    }

    record Active(CosmeticId cosmeticId) implements Selection {
        public Active {
            Objects.requireNonNull(cosmeticId, "cosmeticId");
        }
    }

    record Cleared() implements Selection {
    }
}
