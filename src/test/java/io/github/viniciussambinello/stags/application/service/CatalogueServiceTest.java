package io.github.viniciussambinello.stags.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.github.viniciussambinello.stags.domain.catalogue.ValidationError;
import io.github.viniciussambinello.stags.domain.catalogue.ValidationResult;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.testkit.InMemoryCosmeticRepository;

final class CatalogueServiceTest {

    @Test
    void createAddsToCatalogueAfterSuccessfulWrite() throws Exception {
        final CatalogueService service = new CatalogueService(new InMemoryCosmeticRepository());
        service.loadInitial().get();

        final ValidationResult result = service.create(CosmeticKind.TAG, "vip", "<gold>[VIP]</gold>", "stags.tag.vip", 100).get();

        assertInstanceOf(ValidationResult.Accepted.class, result);
        assertTrue(service.catalogue().contains(CosmeticKind.TAG, new CosmeticId("vip")));
    }

    @Test
    void editUnknownCosmeticReturnsEmpty() throws Exception {
        final CatalogueService service = new CatalogueService(new InMemoryCosmeticRepository());
        service.loadInitial().get();

        assertTrue(service.edit(CosmeticKind.TAG, new CosmeticId("missing"), "<gold>x</gold>", "perm", 1).isEmpty());
    }

    @Test
    void deleteRemovesFromCatalogue() throws Exception {
        final CatalogueService service = new CatalogueService(new InMemoryCosmeticRepository());
        service.loadInitial().get();
        service.create(CosmeticKind.TAG, "vip", "<gold>[VIP]</gold>", "stags.tag.vip", 100).get();

        service.delete(CosmeticKind.TAG, new CosmeticId("vip")).get();

        assertTrue(service.catalogue().all(CosmeticKind.TAG).isEmpty());
    }

    @Test
    void concurrentCreateRaceYieldsExactlyOneWinner() throws Exception {
        final CatalogueService service = new CatalogueService(new InMemoryCosmeticRepository());
        service.loadInitial().get();

        final int attempts = 8;
        final ExecutorService pool = Executors.newFixedThreadPool(attempts);
        final CountDownLatch ready = new CountDownLatch(attempts);
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicInteger accepted = new AtomicInteger();
        final AtomicInteger duplicates = new AtomicInteger();

        final CompletableFuture<?>[] futures = new CompletableFuture[attempts];
        for (int index = 0; index < attempts; index++) {
            futures[index] = CompletableFuture.runAsync(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (final InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                final ValidationResult result = service.create(CosmeticKind.TAG, "vip", "<gold>[VIP]</gold>", "stags.tag.vip", 100).join();
                if (result instanceof ValidationResult.Accepted) {
                    accepted.incrementAndGet();
                } else if (result instanceof ValidationResult.Rejected rejected
                        && rejected.error() instanceof ValidationError.DuplicateIdentifier) {
                    duplicates.incrementAndGet();
                }
            }, pool);
        }

        ready.await();
        start.countDown();
        CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(1, accepted.get());
        assertEquals(attempts - 1, duplicates.get());
    }
}
