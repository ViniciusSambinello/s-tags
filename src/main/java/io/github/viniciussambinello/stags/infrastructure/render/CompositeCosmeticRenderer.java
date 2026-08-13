package io.github.viniciussambinello.stags.infrastructure.render;

import java.util.List;
import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.entity.Player;

import io.github.viniciussambinello.stags.application.port.CosmeticRenderer;

public final class CompositeCosmeticRenderer implements CosmeticRenderer {

    private final Server server;
    private final List<TargetRenderer> targets;

    public CompositeCosmeticRenderer(
            final Server server, final NametagRenderAdapter nametagRenderAdapter, final TabListRenderAdapter tabListRenderAdapter) {
        this.server = server;
        this.targets = List.of(nametagRenderAdapter, tabListRenderAdapter);
    }

    @Override
    public void refresh(final UUID playerId) {
        final Player player = server.getPlayer(playerId);
        if (player == null) {
            return;
        }
        targets.forEach(target -> target.refresh(player));
    }

    public void teardown(final Player player) {
        targets.forEach(target -> target.teardown(player));
    }

    public void shutdown() {
        targets.forEach(TargetRenderer::shutdown);
    }
}
