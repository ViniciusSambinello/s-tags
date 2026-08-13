package io.github.viniciussambinello.stags.infrastructure.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
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

final class NametagRenderAdapterTest {

    private final Map<String, Team> registeredTeams = new HashMap<>();

    private Scoreboard mockScoreboard() {
        final Scoreboard scoreboard = Mockito.mock(Scoreboard.class);
        Mockito.when(scoreboard.registerNewTeam(anyString())).thenAnswer(invocation -> {
            final String name = invocation.getArgument(0);
            final Team team = Mockito.mock(Team.class);
            Mockito.when(team.getName()).thenReturn(name);
            final Map<String, Boolean> entries = new HashMap<>();
            Mockito.doAnswer(inv -> entries.put(inv.getArgument(0), true)).when(team).addEntry(anyString());
            Mockito.doAnswer(inv -> entries.remove(inv.getArgument(0))).when(team).removeEntry(anyString());
            Mockito.when(team.hasEntry(anyString())).thenAnswer(inv -> entries.containsKey(inv.getArgument(0)));
            registeredTeams.put(name, team);
            return team;
        });
        Mockito.when(scoreboard.getTeam(anyString())).thenAnswer(invocation -> registeredTeams.get((String) invocation.getArgument(0)));
        Mockito.when(scoreboard.getEntryTeam(anyString())).thenAnswer(invocation -> {
            final String entry = invocation.getArgument(0);
            return registeredTeams.values().stream().filter(team -> team.hasEntry(entry)).findFirst().orElse(null);
        });
        return scoreboard;
    }

    private Server mockServer(final Scoreboard scoreboard) {
        final Server server = Mockito.mock(Server.class);
        final ScoreboardManager manager = Mockito.mock(ScoreboardManager.class);
        Mockito.when(manager.getMainScoreboard()).thenReturn(scoreboard);
        Mockito.when(server.getScoreboardManager()).thenReturn(manager);
        return server;
    }

    private Player mockPlayer(final UUID id, final String name) {
        final Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(id);
        Mockito.when(player.getName()).thenReturn(name);
        return player;
    }

    private ConfigService realConfigService(final Path dir) {
        return ConfigService.initial(dir, Logger.getLogger("test"));
    }

    @Test
    void assignsThePlayerToATeamCarryingTheirTagPrefix(@TempDir final Path dir) throws Exception {
        final InMemoryCosmeticRepository cosmeticRepository = new InMemoryCosmeticRepository();
        final Cosmetic vip = new Cosmetic(
                CosmeticKind.TAG, new CosmeticId("vip"), Prefix.parse("<gold>[VIP]</gold>"),
                new PermissionNode("stags.tag.vip"), new Weight(100));
        cosmeticRepository.insert(vip).get();
        final CatalogueService catalogueService = new CatalogueService(cosmeticRepository);
        catalogueService.loadInitial().get();

        final UUID playerId = UUID.randomUUID();
        final InMemorySelectionRepository selectionRepository = new InMemorySelectionRepository();
        selectionRepository.put(new PlayerCosmetics(playerId, new Selection.Active(new CosmeticId("vip")), Selection.UNSET));
        final PlayerCosmeticService playerCosmeticService = new PlayerCosmeticService(selectionRepository);
        playerCosmeticService.load(playerId).get();

        final ActiveCosmeticResolver resolver = new ActiveCosmeticResolver(
                catalogueService, playerCosmeticService, new StubPermissionOracle().grant(playerId, "stags.tag.vip"));
        final Scoreboard scoreboard = mockScoreboard();
        final Server server = mockServer(scoreboard);
        final ConfigService configService = realConfigService(dir);
        final NametagRenderAdapter adapter = new NametagRenderAdapter(configService, resolver, server);
        final Player player = mockPlayer(playerId, "Steve");

        adapter.refresh(player);

        assertEquals(1, registeredTeams.size());
        final Team team = registeredTeams.values().iterator().next();
        Mockito.verify(team).addEntry("Steve");
        Mockito.verify(team).prefix(vip.prefix().rendered());
    }

    @Test
    void disabledTargetTearsDownRatherThanAssigning(@TempDir final Path dir) throws Exception {
        final Path configFile = dir.resolve("config.yml");
        final ConfigService bootstrap = realConfigService(dir);
        final String content = Files.readString(configFile, StandardCharsets.UTF_8).replace("nametag:\n    enabled: true", "nametag:\n    enabled: false");
        Files.writeString(configFile, content, StandardCharsets.UTF_8);
        bootstrap.reload();
        assertTrue(!bootstrap.config().render().nametagEnabled());

        final CatalogueService catalogueService = new CatalogueService(new InMemoryCosmeticRepository());
        catalogueService.loadInitial().get();
        final PlayerCosmeticService playerCosmeticService = new PlayerCosmeticService(new InMemorySelectionRepository());
        final UUID playerId = UUID.randomUUID();
        playerCosmeticService.load(playerId).get();

        final ActiveCosmeticResolver resolver =
                new ActiveCosmeticResolver(catalogueService, playerCosmeticService, new StubPermissionOracle());
        final Scoreboard scoreboard = mockScoreboard();
        final Server server = mockServer(scoreboard);
        final NametagRenderAdapter adapter = new NametagRenderAdapter(bootstrap, resolver, server);
        final Player player = mockPlayer(playerId, "Steve");

        adapter.refresh(player);

        assertEquals(0, registeredTeams.size());
    }
}
