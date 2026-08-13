package io.github.viniciussambinello.stags.infrastructure.command;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.logging.Logger;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.infrastructure.bootstrap.StagsPlugin;
import io.github.viniciussambinello.stags.infrastructure.config.ConfigService;
import io.github.viniciussambinello.stags.infrastructure.selector.SelectorGateway;
import net.kyori.adventure.text.Component;

final class SelectorCommandsTest {

    private StagsPlugin mockPlugin(final ConfigService configService, final SelectorGateway gateway) {
        final StagsPlugin plugin = Mockito.mock(StagsPlugin.class);
        Mockito.when(plugin.configService()).thenReturn(configService);
        Mockito.when(plugin.selectorGateway()).thenReturn(gateway);
        return plugin;
    }

    private CommandSourceStack sourceFor(final CommandSender sender) {
        final CommandSourceStack source = Mockito.mock(CommandSourceStack.class);
        Mockito.when(source.getSender()).thenReturn(sender);
        return source;
    }

    @Test
    void playerExecutingTagOpensTheirSelector(@TempDir final Path dir) throws Exception {
        final ConfigService configService = ConfigService.initial(dir, Logger.getLogger("test"));
        final SelectorGateway gateway = Mockito.mock(SelectorGateway.class);
        final StagsPlugin plugin = mockPlugin(configService, gateway);

        final SelectorCommands commands = new SelectorCommands(() -> plugin);
        final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.getRoot().addChild(commands.tagCommand());

        final Player player = Mockito.mock(Player.class);
        Mockito.when(player.hasPermission("stags.command.tag")).thenReturn(true);

        dispatcher.execute("tag", sourceFor(player));

        Mockito.verify(gateway).open(player, CosmeticKind.TAG);
    }

    @Test
    void consoleExecutingTagReceivesPlayerOnlyMessageRatherThanOpening(@TempDir final Path dir) throws Exception {
        final ConfigService configService = ConfigService.initial(dir, Logger.getLogger("test"));
        final SelectorGateway gateway = Mockito.mock(SelectorGateway.class);
        final StagsPlugin plugin = mockPlugin(configService, gateway);

        final SelectorCommands commands = new SelectorCommands(() -> plugin);
        final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.getRoot().addChild(commands.titleCommand());

        final ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        Mockito.when(console.hasPermission("stags.command.title")).thenReturn(true);

        dispatcher.execute("title", sourceFor(console));

        Mockito.verify(console).sendMessage(Mockito.any(Component.class));
        Mockito.verify(gateway, Mockito.never()).open(Mockito.any(), Mockito.any());
    }

    @Test
    void withoutPermissionTheCommandIsNotFoundAtAll(@TempDir final Path dir) throws Exception {
        final ConfigService configService = ConfigService.initial(dir, Logger.getLogger("test"));
        final SelectorGateway gateway = Mockito.mock(SelectorGateway.class);
        final StagsPlugin plugin = mockPlugin(configService, gateway);

        final SelectorCommands commands = new SelectorCommands(() -> plugin);
        final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.getRoot().addChild(commands.tagCommand());

        final Player player = Mockito.mock(Player.class);
        Mockito.when(player.hasPermission("stags.command.tag")).thenReturn(false);

        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("tag", sourceFor(player)));
    }
}
