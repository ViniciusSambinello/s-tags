package io.github.viniciussambinello.stags.infrastructure.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.bukkit.plugin.Plugin;

public final class MainThreadDispatcher {

    private final Plugin plugin;

    public MainThreadDispatcher(final Plugin plugin) {
        this.plugin = plugin;
    }

    public void run(final Runnable task) {
        if (plugin.getServer().isPrimaryThread()) {
            task.run();
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    public <T> CompletableFuture<T> supply(final Supplier<T> supplier) {
        if (plugin.getServer().isPrimaryThread()) {
            return CompletableFuture.completedFuture(supplier.get());
        }
        final CompletableFuture<T> future = new CompletableFuture<>();
        plugin.getServer().getScheduler().runTask(plugin, () -> future.complete(supplier.get()));
        return future;
    }
}
