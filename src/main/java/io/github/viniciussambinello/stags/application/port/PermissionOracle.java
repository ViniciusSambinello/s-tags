package io.github.viniciussambinello.stags.application.port;

import java.util.UUID;

public interface PermissionOracle {

    boolean hasPermission(UUID playerId, String permissionNode);
}
