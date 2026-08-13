package io.github.viniciussambinello.stags.infrastructure.storage.yaml;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.viniciussambinello.stags.infrastructure.concurrent.StorageExecutor;

public final class DebouncedWriter implements AutoCloseable {

    private final StorageExecutor storageExecutor;
    private final ScheduledExecutorService scheduler;
    private final Duration interval;
    private final Runnable flushTask;
    private final AtomicBoolean dirty;

    public DebouncedWriter(final Duration interval, final StorageExecutor storageExecutor, final Runnable flushTask) {
        this.interval = interval;
        this.storageExecutor = storageExecutor;
        this.flushTask = flushTask;
        this.dirty = new AtomicBoolean(false);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "s-tags-yaml-debounce");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void markDirty() {
        if (dirty.compareAndSet(false, true)) {
            scheduler.schedule(this::triggerFlush, interval.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    public CompletableFuture<Void> flushNow() {
        if (dirty.compareAndSet(true, false)) {
            return storageExecutor.run(flushTask);
        }
        return CompletableFuture.completedFuture(null);
    }

    private void triggerFlush() {
        if (dirty.compareAndSet(true, false)) {
            storageExecutor.run(flushTask);
        }
    }

    @Override
    public void close() {
        try {
            flushNow().get(10, TimeUnit.SECONDS);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (final CompletionException | ExecutionException | TimeoutException exception) {
        }
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
