package io.github.viniciussambinello.stags.application.port;

import java.util.UUID;

public interface CosmeticRenderer {

    void refresh(UUID playerId);
}
