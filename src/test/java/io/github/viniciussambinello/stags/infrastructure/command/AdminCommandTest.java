package io.github.viniciussambinello.stags.infrastructure.command;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.logging.Logger;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import io.github.viniciussambinello.stags.application.service.ActiveCosmeticResolver;
import io.github.viniciussambinello.stags.application.service.CatalogueService;
import io.github.viniciussambinello.stags.application.service.PlayerCosmeticService;
import io.github.viniciussambinello.stags.application.usecase.SelectCosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.cosmetic.PermissionNode;
import io.github.viniciussambinello.stags.domain.cosmetic.Prefix;
import io.github.viniciussambinello.stags.domain.cosmetic.Weight;
import io.github.viniciussambinello.stags.infrastructure.bootstrap.StagsPlugin;
import io.github.viniciussambinello.stags.infrastructure.concurrent.MainThreadDispatcher;
import io.github.viniciussambinello.stags.infrastructure.config.ConfigService;
import io.github.viniciussambinello.stags.testkit.InMemoryCosmeticRepository;
import io.github.viniciussambinello.stags.testkit.InMemorySelectionRepository;
import io.github.viniciussambinello.stags.testkit.RecordingCosmeticRenderer;
import io.github.viniciussambinello.stags.testkit.StubPermissionOracle;
import net.kyori.adventure.text.Component;

final class AdminCommandTest {

    private record Fixture(StagsPlugin plugin, CatalogueService catalogueService, ConfigService configService) {
    }

    private Fixture buildFixture(final Path dir) throws Exception {
        final InMemoryCosmeticRepository cosmeticRepository = new InMemoryCosmeticRepository();
        final CatalogueService catalogueService = new CatalogueService(cosmeticRepository);
        catalogueService.loadInitial().get();
        final PlayerCosmeticService playerCosmeticService = new PlayerCosmeticService(new InMemorySelectionRepository());
        final ConfigService configService = ConfigService.initial(dir, Logger.getLogger("test"));
        final StubPermissionOracle permissions = new StubPermissionOracle();
        final ActiveCosmeticResolver activeCosmeticResolver =
                new ActiveCosmeticResolver(catalogueService, playerCosmeticService, permissions);
        final SelectCosmetic selectCosmetic =
                new SelectCosmetic(catalogueService, playerCosmeticService, permissions, new RecordingCosmeticRenderer());
        final MainThreadDispatcher dispatcher = Mockito.mock(MainThreadDispatcher.class);
        Mockito.doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(dispatcher).run(Mockito.any());

        final StagsPlugin plugin = Mockito.mock(StagsPlugin.class);
        Mockito.when(plugin.configService()).thenReturn(configService);
        Mockito.when(plugin.catalogueService()).thenReturn(catalogueService);
        Mockito.when(plugin.playerCosmeticService()).thenReturn(playerCosmeticService);
        Mockito.when(plugin.activeCosmeticResolver()).thenReturn(activeCosmeticResolver);
        Mockito.when(plugin.selectCosmetic()).thenReturn(selectCosmetic);
        Mockito.when(plugin.mainThreadDispatcher()).thenReturn(dispatcher);

        return new Fixture(plugin, catalogueService, configService);
    }

    private CommandDispatcher<CommandSourceStack> dispatcherFor(final StagsPlugin plugin) {
        final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.getRoot().addChild(new AdminCommand(() -> plugin).build());
        return dispatcher;
    }

    private CommandSourceStack sourceFor(final CommandSender sender) {
        final CommandSourceStack source = Mockito.mock(CommandSourceStack.class);
        Mockito.when(source.getSender()).thenReturn(sender);
        return source;
    }

    private ConsoleCommandSender opConsole() {
        final ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        Mockito.when(console.hasPermission(Mockito.anyString())).thenReturn(true);
        return console;
    }

    @Test
    void listWithNoEntriesSendsTheEmptyMessage(@TempDir final Path dir) throws Exception {
        final Fixture fixture = buildFixture(dir);
        final ConsoleCommandSender console = opConsole();

        dispatcherFor(fixture.plugin()).execute("stags list tag", sourceFor(console));

        final ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        Mockito.verify(console, Mockito.atLeastOnce()).sendMessage(captor.capture());
    }

    @Test
    void listWithEntriesSendsOneLinePerCosmetic(@TempDir final Path dir) throws Exception {
        final Fixture fixture = buildFixture(dir);
        fixture.catalogueService().create(CosmeticKind.TAG, "vip", "<gold>[VIP]</gold>", "stags.tag.vip", 100).get();
        final ConsoleCommandSender console = opConsole();

        dispatcherFor(fixture.plugin()).execute("stags list tag", sourceFor(console));

        Mockito.verify(console, Mockito.atLeastOnce()).sendMessage(Mockito.any(Component.class));
    }

    @Test
    void reloadSucceedsAndReportsSuccessMessage(@TempDir final Path dir) throws Exception {
        final Fixture fixture = buildFixture(dir);
        Mockito.when(fixture.plugin().reloadConfiguration())
                .thenReturn(new ConfigService.ReloadOutcome.Success(false));
        final ConsoleCommandSender console = opConsole();

        dispatcherFor(fixture.plugin()).execute("stags reload", sourceFor(console));

        Mockito.verify(console, Mockito.atLeastOnce()).sendMessage(Mockito.any(Component.class));
    }

    @Test
    void createFromConsoleIsRefusedAsPlayerOnly(@TempDir final Path dir) throws Exception {
        final Fixture fixture = buildFixture(dir);
        final ConsoleCommandSender console = opConsole();

        dispatcherFor(fixture.plugin()).execute("stags create tag", sourceFor(console));

        Mockito.verify(console, Mockito.atLeastOnce()).sendMessage(Mockito.any(Component.class));
    }

    @Test
    void withoutAdminPermissionTheSubcommandIsNotFound(@TempDir final Path dir) throws Exception {
        final Fixture fixture = buildFixture(dir);
        final ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        Mockito.when(console.hasPermission(Mockito.anyString())).thenReturn(false);

        assertThrows(CommandSyntaxException.class,
                () -> dispatcherFor(fixture.plugin()).execute("stags list tag", sourceFor(console)));
    }

    @Test
    void forceUnownedIsBlockedByDefault(@TempDir final Path dir) throws Exception {
        final Fixture fixture = buildFixture(dir);
        fixture.catalogueService().create(CosmeticKind.TAG, "vip", "<gold>[VIP]</gold>", "stags.tag.vip", 100).get();
        final ConsoleCommandSender console = opConsole();

        final Player target = Mockito.mock(Player.class);
        Mockito.when(target.getName()).thenReturn("Steve");
        Mockito.when(target.hasPermission("stags.tag.vip")).thenReturn(false);
        try (var mocked = Mockito.mockStatic(org.bukkit.Bukkit.class)) {
            mocked.when(() -> org.bukkit.Bukkit.getPlayerExact("Steve")).thenReturn(target);
            dispatcherFor(fixture.plugin()).execute("stags force Steve tag vip", sourceFor(console));
        }

        Mockito.verify(console, Mockito.atLeastOnce()).sendMessage(Mockito.any(Component.class));
    }
}
