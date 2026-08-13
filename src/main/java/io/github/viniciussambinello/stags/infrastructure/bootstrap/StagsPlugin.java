package io.github.viniciussambinello.stags.infrastructure.bootstrap;

import org.bukkit.plugin.java.JavaPlugin;

public final class StagsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("s-tags " + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("s-tags " + getPluginMeta().getVersion() + " disabled.");
    }
}
