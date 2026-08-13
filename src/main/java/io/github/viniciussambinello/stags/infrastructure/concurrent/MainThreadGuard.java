package io.github.viniciussambinello.stags.infrastructure.concurrent;

import org.bukkit.Server;

public final class MainThreadGuard {

    private final Server server;

    public MainThreadGuard(final Server server) {
        this.server = server;
    }

    public void assertOffMainThread(final String operation) {
        if (server.isPrimaryThread()) {
            throw new IllegalStateException("Storage operation '" + operation + "' must not run on the server main thread");
        }
    }
}
