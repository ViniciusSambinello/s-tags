package io.github.viniciussambinello.stags.application.port;

import java.util.concurrent.CompletableFuture;

import io.github.viniciussambinello.stags.domain.catalogue.Catalogue;
import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;

public interface CosmeticRepository {

    enum InsertOutcome {
        CREATED,
        DUPLICATE
    }

    CompletableFuture<Catalogue> loadAll();

    CompletableFuture<InsertOutcome> insert(Cosmetic cosmetic);

    CompletableFuture<Void> update(Cosmetic cosmetic);

    CompletableFuture<Void> delete(CosmeticKind kind, CosmeticId id);
}
