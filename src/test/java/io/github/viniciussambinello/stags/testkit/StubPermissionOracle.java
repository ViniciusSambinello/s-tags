package io.github.viniciussambinello.stags.testkit;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import io.github.viniciussambinello.stags.application.port.PermissionOracle;

public final class StubPermissionOracle implements PermissionOracle {

    private final Set<String> granted = new HashSet<>();

    public StubPermissionOracle grant(final UUID playerId, final String permissionNode) {
        granted.add(playerId + ":" + permissionNode);
        return this;
    }

    @Override
    public boolean hasPermission(final UUID playerId, final String permissionNode) {
        return granted.contains(playerId + ":" + permissionNode);
    }
}
