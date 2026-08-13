package io.github.viniciussambinello.stags.infrastructure.render;

import org.bukkit.entity.Player;

interface TargetRenderer {

    void refresh(Player player);

    void teardown(Player player);

    void shutdown();
}
