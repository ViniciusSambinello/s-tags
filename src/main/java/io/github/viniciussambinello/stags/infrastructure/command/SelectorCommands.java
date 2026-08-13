package io.github.viniciussambinello.stags.infrastructure.command;

import java.util.function.Supplier;

import org.bukkit.entity.Player;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.infrastructure.bootstrap.StagsPlugin;

public final class SelectorCommands {

    private final Supplier<StagsPlugin> pluginSupplier;

    public SelectorCommands(final Supplier<StagsPlugin> pluginSupplier) {
        this.pluginSupplier = pluginSupplier;
    }

    public LiteralCommandNode<CommandSourceStack> tagCommand() {
        return selectorCommand("tag", "stags.command.tag", CosmeticKind.TAG);
    }

    public LiteralCommandNode<CommandSourceStack> titleCommand() {
        return selectorCommand("title", "stags.command.title", CosmeticKind.TITLE);
    }

    private LiteralCommandNode<CommandSourceStack> selectorCommand(
            final String name, final String permission, final CosmeticKind kind) {
        return Commands.literal(name)
                .requires(source -> source.getSender().hasPermission(permission))
                .executes(context -> {
                    final StagsPlugin plugin = pluginSupplier.get();
                    if (!(context.getSource().getSender() instanceof Player player)) {
                        CommandMessages.sendPlayerOnly(plugin.configService(), context.getSource().getSender());
                        return 0;
                    }
                    plugin.selectorGateway().open(player, kind);
                    return 1;
                })
                .build();
    }
}
