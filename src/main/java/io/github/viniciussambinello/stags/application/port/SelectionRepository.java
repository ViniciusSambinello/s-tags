package io.github.viniciussambinello.stags.application.port;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.player.PlayerCosmetics;
import io.github.viniciussambinello.stags.domain.player.Selection;

public interface SelectionRepository {

    CompletableFuture<PlayerCosmetics> load(UUID playerId);

    CompletableFuture<Void> save(UUID playerId, CosmeticKind kind, Selection selection);
}
