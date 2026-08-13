package io.github.viniciussambinello.stags.application.service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import io.github.viniciussambinello.stags.application.port.SelectionRepository;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.player.PlayerCosmetics;
import io.github.viniciussambinello.stags.domain.player.Selection;

public final class PlayerCosmeticService {

    private final SelectionRepository repository;
    private final ConcurrentHashMap<UUID, PlayerCosmetics> cache;
    private final ConcurrentHashMap<UUID, CompletableFuture<Boolean>> pendingWrites;

    public PlayerCosmeticService(final SelectionRepository repository) {
        this.repository = repository;
        this.cache = new ConcurrentHashMap<>();
        this.pendingWrites = new ConcurrentHashMap<>();
    }

    public CompletableFuture<PlayerCosmetics> load(final UUID playerId) {
        final PlayerCosmetics cached = cache.get(playerId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return repository.load(playerId).thenApply(loaded -> {
            cache.put(playerId, loaded);
            return loaded;
        });
    }

    public Optional<PlayerCosmetics> cached(final UUID playerId) {
        return Optional.ofNullable(cache.get(playerId));
    }

    public CompletableFuture<Void> awaitPending(final UUID playerId) {
        final CompletableFuture<Boolean> pending = pendingWrites.get(playerId);
        return pending == null ? CompletableFuture.completedFuture(null) : pending.handle((result, throwable) -> null);
    }

    public void release(final UUID playerId) {
        cache.remove(playerId);
        pendingWrites.remove(playerId);
    }

    public CompletableFuture<Boolean> updateSelection(final UUID playerId, final CosmeticKind kind, final Selection selection) {
        final PlayerCosmetics current = cache.get(playerId);
        if (current == null) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
        if (current.selection(kind).equals(selection)) {
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }

        final PlayerCosmetics updated = current.withSelection(kind, selection);
        cache.put(playerId, updated);

        final CompletableFuture<Boolean> write = repository.save(playerId, kind, selection).handle((unused, throwable) -> {
            if (throwable != null) {
                cache.put(playerId, current);
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        });
        pendingWrites.put(playerId, write);
        return write;
    }
}
