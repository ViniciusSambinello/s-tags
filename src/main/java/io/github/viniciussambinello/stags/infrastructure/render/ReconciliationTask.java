package io.github.viniciussambinello.stags.infrastructure.render;

import org.bukkit.Server;

public final class ReconciliationTask implements Runnable {

    private final Server server;
    private final CompositeCosmeticRenderer renderer;

    public ReconciliationTask(final Server server, final CompositeCosmeticRenderer renderer) {
        this.server = server;
        this.renderer = renderer;
    }

    @Override
    public void run() {
        server.getOnlinePlayers().forEach(player -> renderer.refresh(player.getUniqueId()));
    }
}
