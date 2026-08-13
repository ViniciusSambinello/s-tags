package io.github.viniciussambinello.stags.infrastructure.command;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import io.github.viniciussambinello.stags.application.usecase.SelectCosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.infrastructure.bootstrap.StagsPlugin;
import io.github.viniciussambinello.stags.infrastructure.config.CommandConfig;
import io.github.viniciussambinello.stags.infrastructure.config.ConfigService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public final class AdminCommand {

    private final Supplier<StagsPlugin> pluginSupplier;

    public AdminCommand(final Supplier<StagsPlugin> pluginSupplier) {
        this.pluginSupplier = pluginSupplier;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("stags")
                .then(Commands.literal("create")
                        .requires(source -> source.getSender().hasPermission("stags.admin.create"))
                        .then(kindArgument()
                                .executes(this::executeCreate)))
                .then(Commands.literal("edit")
                        .requires(source -> source.getSender().hasPermission("stags.admin.edit"))
                        .then(kindArgument()
                                .then(identifierArgument()
                                        .executes(this::executeEdit))))
                .then(Commands.literal("delete")
                        .requires(source -> source.getSender().hasPermission("stags.admin.delete"))
                        .then(kindArgument()
                                .then(identifierArgument()
                                        .executes(this::executeDelete))))
                .then(Commands.literal("list")
                        .requires(source -> source.getSender().hasPermission("stags.admin.list"))
                        .then(kindArgument()
                                .executes(this::executeList)))
                .then(Commands.literal("force")
                        .requires(source -> source.getSender().hasPermission("stags.admin.force"))
                        .then(playerArgument()
                                .then(kindArgument()
                                        .then(identifierArgument()
                                                .executes(this::executeForce)))))
                .then(Commands.literal("reload")
                        .requires(source -> source.getSender().hasPermission("stags.admin.reload"))
                        .executes(this::executeReload))
                .build();
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> kindArgument() {
        return Commands.argument("kind", StringArgumentType.word())
                .suggests((context, builder) -> suggest(builder, List.of("tag", "title")));
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> identifierArgument() {
        return Commands.argument("id", StringArgumentType.word())
                .suggests((context, builder) -> {
                    final CosmeticKind kind = parseKind(context);
                    if (kind == null) {
                        return builder.buildFuture();
                    }
                    return suggest(builder, pluginSupplier.get().catalogueService().catalogue().all(kind).stream()
                            .map(cosmetic -> cosmetic.id().value())
                            .toList());
                });
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> playerArgument() {
        return Commands.argument("player", StringArgumentType.word())
                .suggests((context, builder) -> suggest(builder, Bukkit.getOnlinePlayers().stream().map(Player::getName).toList()));
    }

    private CompletableFuture<Suggestions> suggest(final SuggestionsBuilder builder, final List<String> options) {
        final String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(remaining)).forEach(builder::suggest);
        return builder.buildFuture();
    }

    private CosmeticKind parseKind(final CommandContext<CommandSourceStack> context) {
        try {
            return CosmeticKind.valueOf(context.getArgument("kind", String.class).toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    private int executeCreate(final CommandContext<CommandSourceStack> context) {
        final StagsPlugin plugin = pluginSupplier.get();
        final ConfigService configService = plugin.configService();
        final CosmeticKind kind = parseKind(context);
        final CommandSender sender = context.getSource().getSender();
        if (kind == null) {
            CommandMessages.sendUsage(configService, sender, "/stags create <tag|title>");
            return 0;
        }
        if (!(sender instanceof Player player)) {
            CommandMessages.sendPlayerOnly(configService, sender);
            return 0;
        }
        plugin.chatAuthoringFlow().startCreate(player, kind);
        return 1;
    }

    private int executeEdit(final CommandContext<CommandSourceStack> context) {
        final StagsPlugin plugin = pluginSupplier.get();
        final ConfigService configService = plugin.configService();
        final CosmeticKind kind = parseKind(context);
        final CommandSender sender = context.getSource().getSender();
        if (kind == null) {
            CommandMessages.sendUsage(configService, sender, "/stags edit <tag|title> <id>");
            return 0;
        }
        if (!(sender instanceof Player player)) {
            CommandMessages.sendPlayerOnly(configService, sender);
            return 0;
        }
        final String rawId = context.getArgument("id", String.class);
        if (!CosmeticId.isValid(rawId)) {
            CommandMessages.send(configService, sender, "selection.unknown", Placeholder.component("kind", kindComponent(kind)));
            return 0;
        }
        final var cosmetic = plugin.catalogueService().catalogue().find(kind, new CosmeticId(rawId));
        if (cosmetic.isEmpty()) {
            CommandMessages.send(configService, sender, "selection.unknown", Placeholder.component("kind", kindComponent(kind)));
            return 0;
        }
        plugin.chatAuthoringFlow().startEdit(player, cosmetic.get());
        return 1;
    }

    private int executeDelete(final CommandContext<CommandSourceStack> context) {
        final StagsPlugin plugin = pluginSupplier.get();
        final ConfigService configService = plugin.configService();
        final CosmeticKind kind = parseKind(context);
        final CommandSender sender = context.getSource().getSender();
        if (kind == null) {
            CommandMessages.sendUsage(configService, sender, "/stags delete <tag|title> <id>");
            return 0;
        }
        if (!(sender instanceof Player player)) {
            CommandMessages.sendPlayerOnly(configService, sender);
            return 0;
        }
        final String rawId = context.getArgument("id", String.class);
        if (!CosmeticId.isValid(rawId) || plugin.catalogueService().catalogue().find(kind, new CosmeticId(rawId)).isEmpty()) {
            CommandMessages.send(configService, sender, "selection.unknown", Placeholder.component("kind", kindComponent(kind)));
            return 0;
        }
        plugin.chatAuthoringFlow().startDelete(player, kind, new CosmeticId(rawId));
        return 1;
    }

    private int executeList(final CommandContext<CommandSourceStack> context) {
        final StagsPlugin plugin = pluginSupplier.get();
        final ConfigService configService = plugin.configService();
        final CosmeticKind kind = parseKind(context);
        final CommandSender sender = context.getSource().getSender();
        if (kind == null) {
            CommandMessages.sendUsage(configService, sender, "/stags list <tag|title>");
            return 0;
        }
        final List<Cosmetic> all = plugin.catalogueService().catalogue().all(kind);
        if (all.isEmpty()) {
            CommandMessages.send(configService, sender, "selector.empty", Placeholder.component("kind", kindComponent(kind)));
            return 1;
        }
        for (final Cosmetic cosmetic : all) {
            sender.sendMessage(Component.text(cosmetic.id().value() + " (" + cosmetic.weight().value() + ") ")
                    .append(cosmetic.prefix().rendered()));
        }
        return 1;
    }

    private int executeForce(final CommandContext<CommandSourceStack> context) {
        final StagsPlugin plugin = pluginSupplier.get();
        final ConfigService configService = plugin.configService();
        final CosmeticKind kind = parseKind(context);
        final CommandSender sender = context.getSource().getSender();
        if (kind == null) {
            CommandMessages.sendUsage(configService, sender, "/stags force <player> <tag|title> <id>");
            return 0;
        }
        final String rawPlayerName = context.getArgument("player", String.class);
        final Player target = Bukkit.getPlayerExact(rawPlayerName);
        if (target == null) {
            CommandMessages.send(configService, sender, "general.unknown-player");
            return 0;
        }
        final String rawId = context.getArgument("id", String.class);
        if (!CosmeticId.isValid(rawId)) {
            CommandMessages.send(configService, sender, "selection.unknown", Placeholder.component("kind", kindComponent(kind)));
            return 0;
        }
        final CosmeticId id = new CosmeticId(rawId);
        final CommandConfig commandConfig = configService.config().command();
        final boolean owns = plugin.catalogueService().catalogue().find(kind, id)
                .map(cosmetic -> cosmetic.permission().isEmpty() || target.hasPermission(cosmetic.permission().value()))
                .orElse(false);
        if (!owns && !commandConfig.allowForceUnowned()) {
            CommandMessages.send(configService, sender, "command.force-unowned-blocked");
            return 0;
        }
        if (!owns) {
            plugin.getLogger().warning(sender.getName() + " forced " + target.getName() + "'s " + kind.name().toLowerCase(Locale.ROOT)
                    + " to '" + id.value() + "', which they do not own (command.allow-force-unowned is enabled).");
        }
        plugin.selectCosmetic().execute(target.getUniqueId(), kind, id).whenComplete((result, throwable) ->
                plugin.mainThreadDispatcher().run(() -> {
                    if (throwable != null || result instanceof SelectCosmetic.Result.StorageFailure) {
                        CommandMessages.send(configService, sender, "storage.failure");
                        return;
                    }
                    if (result instanceof SelectCosmetic.Result.Applied applied) {
                        CommandMessages.send(configService, target, "command.force-notified",
                                Placeholder.component("kind", kindComponent(kind)),
                                Placeholder.component("name", applied.cosmetic().prefix().rendered()));
                    }
                }));
        return 1;
    }

    private int executeReload(final CommandContext<CommandSourceStack> context) {
        final StagsPlugin plugin = pluginSupplier.get();
        final ConfigService configService = plugin.configService();
        final CommandSender sender = context.getSource().getSender();
        final var outcome = plugin.reloadConfiguration();
        if (outcome instanceof ConfigService.ReloadOutcome.Failure failure) {
            CommandMessages.send(configService, sender, "general.reload-invalid",
                    Placeholder.component("reason", Component.text(String.valueOf(failure.reason()))));
            return 0;
        }
        CommandMessages.send(configService, sender, "general.reload-success");
        if (((ConfigService.ReloadOutcome.Success) outcome).storageRestartRequired()) {
            CommandMessages.send(configService, sender, "general.storage-restart-required");
        }
        return 1;
    }

    private Component kindComponent(final CosmeticKind kind) {
        return Component.text(kind.name().toLowerCase(Locale.ROOT));
    }
}
