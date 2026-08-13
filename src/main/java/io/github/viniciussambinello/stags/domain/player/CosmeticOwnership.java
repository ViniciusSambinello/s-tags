package io.github.viniciussambinello.stags.domain.player;

import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;

@FunctionalInterface
public interface CosmeticOwnership {

    boolean owns(CosmeticId cosmeticId);
}
