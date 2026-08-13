package io.github.viniciussambinello.stags.infrastructure.bootstrap;

import java.util.concurrent.atomic.AtomicReference;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.viniciussambinello.stags.infrastructure.command.AdminCommand;
import io.github.viniciussambinello.stags.infrastructure.command.SelectorCommands;

public final class StagsBootstrap implements PluginBootstrap {

    private final AtomicReference<StagsPlugin> pluginReference = new AtomicReference<>();

    @Override
    public void bootstrap(final BootstrapContext context) {
        final SelectorCommands selectorCommands = new SelectorCommands(pluginReference::get);
        final AdminCommand adminCommand = new AdminCommand(pluginReference::get);

        context.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final var registrar = event.registrar();
            registrar.register(selectorCommands.tagCommand(), "Open the tag selector.");
            registrar.register(selectorCommands.titleCommand(), "Open the title selector.");
            registrar.register(adminCommand.build(), "Administer s-tags cosmetics.");
        });
    }

    @Override
    public JavaPlugin createPlugin(final PluginProviderContext context) {
        final StagsPlugin plugin = new StagsPlugin();
        pluginReference.set(plugin);
        return plugin;
    }
}
