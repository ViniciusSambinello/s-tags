package io.github.viniciussambinello.stags.infrastructure.bootstrap;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import org.bukkit.plugin.java.JavaPlugin;

public final class StagsBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(final BootstrapContext context) {
    }

    @Override
    public JavaPlugin createPlugin(final PluginProviderContext context) {
        return new StagsPlugin();
    }
}
