package io.github.viniciussambinello.stags.infrastructure.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class StorageExecutor implements AutoCloseable {

    private final ExecutorService executor;

    public StorageExecutor() {
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "s-tags-storage");
            thread.setDaemon(true);
            return thread;
        });
    }

    public <T> CompletableFuture<T> submit(final Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor);
    }

    public CompletableFuture<Void> run(final Runnable task) {
        return CompletableFuture.runAsync(task, executor);
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (final InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
