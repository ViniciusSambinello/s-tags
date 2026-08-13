package io.github.viniciussambinello.stags.infrastructure.render;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import io.github.viniciussambinello.stags.application.service.ActiveCosmeticResolver;
import io.github.viniciussambinello.stags.application.service.CatalogueService;
import io.github.viniciussambinello.stags.application.service.PlayerCosmeticService;
import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.cosmetic.PermissionNode;
import io.github.viniciussambinello.stags.domain.cosmetic.Prefix;
import io.github.viniciussambinello.stags.domain.cosmetic.Weight;
import io.github.viniciussambinello.stags.domain.player.PlayerCosmetics;
import io.github.viniciussambinello.stags.domain.player.Selection;
import io.github.viniciussambinello.stags.infrastructure.config.ConfigService;
import io.github.viniciussambinello.stags.testkit.InMemoryCosmeticRepository;
import io.github.viniciussambinello.stags.testkit.InMemorySelectionRepository;
import io.github.viniciussambinello.stags.testkit.StubPermissionOracle;

final class TitleHologramRenderAdapterTest {

    private Plugin mockPlugin() {
        final Plugin plugin = Mockito.mock(Plugin.class);
        final Server server = Mockito.mock(Server.class);
        Mockito.when(plugin.getServer()).thenReturn(server);
        Mockito.when(server.getPluginManager()).thenReturn(Mockito.mock(PluginManager.class));
        return plugin;
    }

    private TextDisplay mockDisplay() {
        final TextDisplay display = Mockito.mock(TextDisplay.class);
        Mockito.when(display.getPersistentDataContainer()).thenReturn(Mockito.mock(PersistentDataContainer.class));
        Mockito.when(display.isValid()).thenReturn(true);
        return display;
    }

    private World mockWorldSpawning(final TextDisplay display) {
        final World world = Mockito.mock(World.class);
        Mockito.when(world.spawn(any(Location.class), eq(TextDisplay.class), any(Consumer.class))).thenAnswer(invocation -> {
            final Consumer<TextDisplay> consumer = invocation.getArgument(2);
            consumer.accept(display);
            return display;
        });
        return world;
    }

    private Player mockPlayer(final UUID id, final String name, final World world) {
        final Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(id);
        Mockito.when(player.getName()).thenReturn(name);
        Mockito.when(player.getWorld()).thenReturn(world);
        Mockito.when(player.getLocation()).thenReturn(Mockito.mock(Location.class));
        Mockito.when(player.getPassengers()).thenReturn(List.of());
        return player;
    }

    private ActiveCosmeticResolver resolverWithActiveTitleFor(
            final UUID playerId, final Cosmetic title, final boolean granted) throws Exception {
        final InMemoryCosmeticRepository cosmeticRepository = new InMemoryCosmeticRepository();
        cosmeticRepository.insert(title).get();
        final CatalogueService catalogueService = new CatalogueService(cosmeticRepository);
        catalogueService.loadInitial().get();

        final InMemorySelectionRepository selectionRepository = new InMemorySelectionRepository();
        selectionRepository.put(new PlayerCosmetics(playerId, Selection.UNSET, new Selection.Active(title.id())));
        final PlayerCosmeticService playerCosmeticService = new PlayerCosmeticService(selectionRepository);
        playerCosmeticService.load(playerId).get();

        final StubPermissionOracle permissions = new StubPermissionOracle();
        if (granted && !title.permission().isEmpty()) {
            permissions.grant(playerId, title.permission().value());
        }
        return new ActiveCosmeticResolver(catalogueService, playerCosmeticService, permissions);
    }

    @Test
    void spawnsAndMountsAHologramCarryingTheTitleText(@TempDir final Path dir) throws Exception {
        final UUID playerId = UUID.randomUUID();
        final Cosmetic champion = new Cosmetic(
                CosmeticKind.TITLE, new CosmeticId("champion"), Prefix.parse("<gold>Champion</gold>"),
                new PermissionNode("stags.title.champion"), new Weight(500));
        final ActiveCosmeticResolver resolver = resolverWithActiveTitleFor(playerId, champion, true);

        final Plugin plugin = mockPlugin();
        final Server server = plugin.getServer();
        final TextDisplay display = mockDisplay();
        final World world = mockWorldSpawning(display);
        final Player owner = mockPlayer(playerId, "Steve", world);
        Mockito.doReturn(Set.of(owner)).when(server).getOnlinePlayers();
        Mockito.when(server.getPlayer(playerId)).thenReturn(owner);
        Mockito.when(owner.canSee(owner)).thenReturn(true);

        final ConfigService configService = ConfigService.initial(dir, Logger.getLogger("test"));
        final TitleHologramRenderAdapter adapter = new TitleHologramRenderAdapter(configService, resolver, new io.github.viniciussambinello.stags.infrastructure.placeholder.NoopPlaceholderResolver(), plugin);

        adapter.refresh(owner);

        Mockito.verify(owner).addPassenger(display);
        Mockito.verify(display).text(champion.prefix().rendered());
        Mockito.verify(owner).hideEntity(plugin, display);
    }

    @Test
    void selfVisibleTrueShowsHologramToOwner(@TempDir final Path dir) throws Exception {
        final UUID playerId = UUID.randomUUID();
        final Cosmetic champion = new Cosmetic(
                CosmeticKind.TITLE, new CosmeticId("champion"), Prefix.parse("<gold>Champion</gold>"),
                PermissionNode.NONE, new Weight(500));
        final ActiveCosmeticResolver resolver = resolverWithActiveTitleFor(playerId, champion, true);

        final Plugin plugin = mockPlugin();
        final Server server = plugin.getServer();
        final TextDisplay display = mockDisplay();
        final World world = mockWorldSpawning(display);
        final Player owner = mockPlayer(playerId, "Steve", world);
        Mockito.doReturn(Set.of(owner)).when(server).getOnlinePlayers();
        Mockito.when(server.getPlayer(playerId)).thenReturn(owner);

        final ConfigService configService = ConfigService.initial(dir, Logger.getLogger("test"));
        final Path configFile = dir.resolve("config.yml");
        final String content = Files.readString(configFile, StandardCharsets.UTF_8).replace("self-visible: false", "self-visible: true");
        Files.writeString(configFile, content, StandardCharsets.UTF_8);
        configService.reload();

        final TitleHologramRenderAdapter adapter = new TitleHologramRenderAdapter(configService, resolver, new io.github.viniciussambinello.stags.infrastructure.placeholder.NoopPlaceholderResolver(), plugin);

        adapter.refresh(owner);

        Mockito.verify(owner).showEntity(plugin, display);
    }

    @Test
    void unownedTitleTearsDownRatherThanSpawning(@TempDir final Path dir) throws Exception {
        final UUID playerId = UUID.randomUUID();
        final Cosmetic champion = new Cosmetic(
                CosmeticKind.TITLE, new CosmeticId("champion"), Prefix.parse("<gold>Champion</gold>"),
                new PermissionNode("stags.title.champion"), new Weight(500));
        final ActiveCosmeticResolver resolver = resolverWithActiveTitleFor(playerId, champion, false);

        final Plugin plugin = mockPlugin();
        final World world = Mockito.mock(World.class);
        final Player owner = mockPlayer(playerId, "Steve", world);

        final ConfigService configService = ConfigService.initial(dir, Logger.getLogger("test"));
        final TitleHologramRenderAdapter adapter = new TitleHologramRenderAdapter(configService, resolver, new io.github.viniciussambinello.stags.infrastructure.placeholder.NoopPlaceholderResolver(), plugin);

        adapter.refresh(owner);

        Mockito.verify(world, Mockito.never()).spawn(any(Location.class), eq(TextDisplay.class), any(Consumer.class));
    }

    @Test
    void cleanupRemovesUntrackedMarkedDisplaysButLeavesForeignEntitiesAlone() throws Exception {
        final Plugin plugin = mockPlugin();
        final Server server = plugin.getServer();
        final ConfigService configService = ConfigService.initial(Files.createTempDirectory("hologram-test"), Logger.getLogger("test"));
        final TitleHologramRenderAdapter adapter = new TitleHologramRenderAdapter(configService, null, new io.github.viniciussambinello.stags.infrastructure.placeholder.NoopPlaceholderResolver(), plugin);

        final Chunk chunk = Mockito.mock(Chunk.class);
        final TextDisplay orphaned = mockDisplay();
        Mockito.when(orphaned.getPersistentDataContainer().getOrDefault(any(), any(), Mockito.eq(false))).thenReturn(true);
        Mockito.when(orphaned.getPersistentDataContainer().get(any(), any())).thenReturn(UUID.randomUUID().toString());

        final Entity foreignEntity = Mockito.mock(Entity.class);

        Mockito.when(chunk.getEntities()).thenReturn(new Entity[] {orphaned, foreignEntity});
        Mockito.when(server.getWorlds()).thenReturn(List.of());

        adapter.cleanupChunk(chunk);

        Mockito.verify(orphaned).remove();
        Mockito.verify(foreignEntity, Mockito.never()).remove();
    }
}
