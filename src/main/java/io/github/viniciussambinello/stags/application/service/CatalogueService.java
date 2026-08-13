package io.github.viniciussambinello.stags.application.service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import io.github.viniciussambinello.stags.application.port.CosmeticRepository;
import io.github.viniciussambinello.stags.domain.catalogue.Catalogue;
import io.github.viniciussambinello.stags.domain.catalogue.CatalogueRules;
import io.github.viniciussambinello.stags.domain.catalogue.ValidationError;
import io.github.viniciussambinello.stags.domain.catalogue.ValidationResult;
import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;

public final class CatalogueService {

    private final CosmeticRepository repository;
    private final CatalogueRules rules;
    private final AtomicReference<Catalogue> catalogue;

    public CatalogueService(final CosmeticRepository repository) {
        this.repository = repository;
        this.rules = new CatalogueRules();
        this.catalogue = new AtomicReference<>(Catalogue.empty());
    }

    public CompletableFuture<Void> loadInitial() {
        return repository.loadAll().thenAccept(catalogue::set);
    }

    public Catalogue catalogue() {
        return catalogue.get();
    }

    public CompletableFuture<ValidationResult> create(
            final CosmeticKind kind,
            final String rawId,
            final String rawPrefix,
            final String rawPermission,
            final int rawWeight) {
        final ValidationResult validation = rules.validateNew(kind, rawId, rawPrefix, rawPermission, rawWeight, catalogue.get());
        if (!(validation instanceof ValidationResult.Accepted accepted)) {
            return CompletableFuture.completedFuture(validation);
        }
        return repository.insert(accepted.cosmetic()).thenApply(outcome -> {
            if (outcome == CosmeticRepository.InsertOutcome.DUPLICATE) {
                return new ValidationResult.Rejected(new ValidationError.DuplicateIdentifier(accepted.cosmetic().id()));
            }
            catalogue.updateAndGet(current -> current.withCosmetic(accepted.cosmetic()));
            return validation;
        });
    }

    public Optional<CompletableFuture<ValidationResult>> edit(
            final CosmeticKind kind,
            final CosmeticId id,
            final String rawPrefix,
            final String rawPermission,
            final int rawWeight) {
        final Optional<Cosmetic> existing = catalogue.get().find(kind, id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        final ValidationResult validation = rules.validateEdit(existing.get(), rawPrefix, rawPermission, rawWeight);
        if (!(validation instanceof ValidationResult.Accepted accepted)) {
            return Optional.of(CompletableFuture.completedFuture(validation));
        }
        return Optional.of(repository.update(accepted.cosmetic()).thenApply(unused -> {
            catalogue.updateAndGet(current -> current.withCosmetic(accepted.cosmetic()));
            return validation;
        }));
    }

    public CompletableFuture<Void> delete(final CosmeticKind kind, final CosmeticId id) {
        return repository.delete(kind, id).thenRun(() -> catalogue.updateAndGet(current -> current.withoutCosmetic(kind, id)));
    }
}
