package io.github.viniciussambinello.stags.testkit;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.github.viniciussambinello.stags.application.port.CosmeticRepository;
import io.github.viniciussambinello.stags.domain.catalogue.Catalogue;
import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;

public final class InMemoryCosmeticRepository implements CosmeticRepository {

    private final Map<CosmeticKind, Map<CosmeticId, Cosmetic>> stored = new EnumMap<>(CosmeticKind.class);
    private boolean failNextOperation;

    public InMemoryCosmeticRepository() {
        for (final CosmeticKind kind : CosmeticKind.values()) {
            stored.put(kind, new HashMap<>());
        }
    }

    public void failNextOperation() {
        this.failNextOperation = true;
    }

    @Override
    public CompletableFuture<Catalogue> loadAll() {
        return CompletableFuture.completedFuture(Catalogue.of(stored.values().stream().flatMap(map -> map.values().stream()).toList()));
    }

    @Override
    public synchronized CompletableFuture<InsertOutcome> insert(final Cosmetic cosmetic) {
        if (consumeFailure()) {
            return CompletableFuture.failedFuture(new IllegalStateException("simulated failure"));
        }
        final Map<CosmeticId, Cosmetic> byId = stored.get(cosmetic.kind());
        if (byId.containsKey(cosmetic.id())) {
            return CompletableFuture.completedFuture(InsertOutcome.DUPLICATE);
        }
        byId.put(cosmetic.id(), cosmetic);
        return CompletableFuture.completedFuture(InsertOutcome.CREATED);
    }

    @Override
    public synchronized CompletableFuture<Void> update(final Cosmetic cosmetic) {
        if (consumeFailure()) {
            return CompletableFuture.failedFuture(new IllegalStateException("simulated failure"));
        }
        stored.get(cosmetic.kind()).put(cosmetic.id(), cosmetic);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public synchronized CompletableFuture<Void> delete(final CosmeticKind kind, final CosmeticId id) {
        if (consumeFailure()) {
            return CompletableFuture.failedFuture(new IllegalStateException("simulated failure"));
        }
        stored.get(kind).remove(id);
        return CompletableFuture.completedFuture(null);
    }

    private synchronized boolean consumeFailure() {
        final boolean result = failNextOperation;
        failNextOperation = false;
        return result;
    }
}
