package io.github.viniciussambinello.stags.infrastructure.permission;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.entity.Player;

import io.github.viniciussambinello.stags.application.port.PermissionOracle;

public final class BukkitPermissionOracle implements PermissionOracle {

    private final Server server;

    public BukkitPermissionOracle(final Server server) {
        this.server = server;
    }

    @Override
    public boolean hasPermission(final UUID playerId, final String permissionNode) {
        final Player player = server.getPlayer(playerId);
        return player != null && player.hasPermission(permissionNode);
    }
}
