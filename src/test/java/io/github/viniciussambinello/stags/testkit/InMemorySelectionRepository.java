package io.github.viniciussambinello.stags.testkit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import io.github.viniciussambinello.stags.application.port.SelectionRepository;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.player.PlayerCosmetics;
import io.github.viniciussambinello.stags.domain.player.Selection;

public final class InMemorySelectionRepository implements SelectionRepository {

    private final Map<UUID, PlayerCosmetics> stored = new ConcurrentHashMap<>();
    private volatile boolean failNextSave;

    public void put(final PlayerCosmetics playerCosmetics) {
        stored.put(playerCosmetics.playerId(), playerCosmetics);
    }

    public void failNextSave() {
        this.failNextSave = true;
    }

    @Override
    public CompletableFuture<PlayerCosmetics> load(final UUID playerId) {
        return CompletableFuture.completedFuture(stored.getOrDefault(playerId, PlayerCosmetics.unset(playerId)));
    }

    @Override
    public CompletableFuture<Void> save(final UUID playerId, final CosmeticKind kind, final Selection selection) {
        if (failNextSave) {
            failNextSave = false;
            return CompletableFuture.failedFuture(new IllegalStateException("simulated failure"));
        }
        stored.compute(playerId, (id, existing) -> {
            final PlayerCosmetics base = existing == null ? PlayerCosmetics.unset(playerId) : existing;
            return base.withSelection(kind, selection);
        });
        return CompletableFuture.completedFuture(null);
    }
}
