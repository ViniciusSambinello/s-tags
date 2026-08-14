package io.github.viniciussambinello.stags.infrastructure.authoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import io.github.viniciussambinello.stags.application.service.CatalogueService;
import io.github.viniciussambinello.stags.application.service.PlayerCosmeticService;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.player.PlayerCosmetics;
import io.github.viniciussambinello.stags.infrastructure.concurrent.MainThreadDispatcher;
import io.github.viniciussambinello.stags.infrastructure.config.ConfigService;
import io.github.viniciussambinello.stags.testkit.InMemoryCosmeticRepository;
import io.github.viniciussambinello.stags.testkit.InMemorySelectionRepository;
import io.github.viniciussambinello.stags.testkit.RecordingCosmeticRenderer;

final class ChatAuthoringFlowTest {

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> now;

        MutableClock(final Instant start) {
            this.now = new AtomicReference<>(start);
        }

        void advance(final Duration duration) {
            now.updateAndGet(instant -> instant.plus(duration));
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }

    private MainThreadDispatcher immediateDispatcher() {
        final MainThreadDispatcher dispatcher = Mockito.mock(MainThreadDispatcher.class);
        Mockito.doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(dispatcher).run(Mockito.any());
        return dispatcher;
    }

    private record Fixture(
            ChatAuthoringFlow flow, CatalogueService catalogueService, PlayerCosmeticService playerCosmeticService,
            AuthoringSessionStore sessionStore, Server server, MutableClock clock) {
    }

    private Fixture buildFixture(final Path dir) throws Exception {
        final InMemoryCosmeticRepository cosmeticRepository = new InMemoryCosmeticRepository();
        final CatalogueService catalogueService = new CatalogueService(cosmeticRepository);
        catalogueService.loadInitial().get();
        final PlayerCosmeticService playerCosmeticService = new PlayerCosmeticService(new InMemorySelectionRepository());
        final RecordingCosmeticRenderer renderer = new RecordingCosmeticRenderer();
        final io.github.viniciussambinello.stags.testkit.StubPermissionOracle permissions =
                new io.github.viniciussambinello.stags.testkit.StubPermissionOracle();
        final io.github.viniciussambinello.stags.application.service.ActiveCosmeticResolver activeCosmeticResolver =
                new io.github.viniciussambinello.stags.application.service.ActiveCosmeticResolver(
                        catalogueService, playerCosmeticService, permissions);
        final ConfigService configService = ConfigService.initial(dir, Logger.getLogger("test"));
        final MutableClock clock = new MutableClock(Instant.EPOCH);
        final AuthoringSessionStore sessionStore = new AuthoringSessionStore(clock);
        final Server server = Mockito.mock(Server.class);
        Mockito.doReturn(Set.of()).when(server).getOnlinePlayers();
        final ChatAuthoringFlow flow = new ChatAuthoringFlow(
                configService, catalogueService, playerCosmeticService, activeCosmeticResolver, renderer, sessionStore,
                immediateDispatcher(), server, clock);
        return new Fixture(flow, catalogueService, playerCosmeticService, sessionStore, server, clock);
    }

    private Player mockPlayer(final UUID id) {
        final Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(id);
        Mockito.when(player.isOnline()).thenReturn(true);
        return player;
    }

    @Test
    void happyPathCreatesACosmeticWithDefaultedPermission(@TempDir final Path dir) throws Exception {
        final Fixture fixture = buildFixture(dir);
        final UUID playerId = UUID.randomUUID();
        final Player player = mockPlayer(playerId);

        fixture.flow().startCreate(player, CosmeticKind.TAG);
        fixture.flow().handleInput(player, "vip");
        fixture.flow().handleInput(player, "<gold>[VIP]</gold>");
        fixture.flow().handleInput(player, "skip");
        fixture.flow().handleInput(player, "100");
        fixture.flow().handleInput(player, "confirm");

        assertTrue(fixture.catalogueService().catalogue().contains(CosmeticKind.TAG, new CosmeticId("vip")));
        final var cosmetic = fixture.catalogueService().catalogue().find(CosmeticKind.TAG, new CosmeticId("vip")).orElseThrow();
        assertEquals("stags.tag.vip", cosmetic.permission().value());
        assertEquals(100, cosmetic.weight().value());
        assertTrue(fixture.sessionStore().find(playerId).isEmpty());
    }

    @Test
    void legacyColorCodePrefixSurvivesConfirmationsCommitTimeRevalidation(@TempDir final Path dir) throws Exception {
        final Fixture fixture = buildFixture(dir);
        final UUID playerId = UUID.randomUUID();
        final Player player = mockPlayer(playerId);

        fixture.flow().startCreate(player, CosmeticKind.TAG);
        fixture.flow().handleInput(player, "owner");
        fixture.flow().handleInput(player, "&c[Owner]");
        fixture.flow().handleInput(player, "skip");
        fixture.flow().handleInput(player, "100");
        fixture.flow().handleInput(player, "confirm");

        assertTrue(fixture.catalogueService().catalogue().contains(CosmeticKind.TAG, new CosmeticId("owner")));
        assertTrue(fixture.sessionStore().find(playerId).isEmpty());
    }

    @Test
    void cancelAtAnyStepDiscardsSessionAndPersistsNothing(@TempDir final Path dir) throws Exception {
        final Fixture fixture = buildFixture(dir);
        final UUID playerId = UUID.randomUUID();
        final Player player = mockPlayer(playerId);

        fixture.flow().startCreate(player, CosmeticKind.TAG);
        fixture.flow().handleInput(player, "vip");
        fixture.flow().handleInput(player, "cancel");

        assertTrue(fixture.sessionStore().find(playerId).isEmpty());
        assertTrue(fixture.catalogueService().catalogue().all(CosmeticKind.TAG).isEmpty());
    }

    @Test
    void duplicateIdentifierReportsErrorAndRetriesSameStep(@TempDir final Path dir) throws Exception {
        final Fixture fixture = buildFixture(dir);
        fixture.catalogueService().create(CosmeticKind.TAG, "vip", "<gold>[VIP]</gold>", "stags.tag.vip", 100).get();

        final UUID playerId = UUID.randomUUID();
        final Player player = mockPlayer(playerId);
        fixture.flow().startCreate(player, CosmeticKind.TAG);
        fixture.flow().handleInput(player, "vip");

        assertTrue(fixture.sessionStore().find(playerId).orElseThrow().step()
                instanceof AuthoringStep.CreateAwaitingIdentifier);

        fixture.flow().handleInput(player, "member");
        assertTrue(fixture.sessionStore().find(playerId).orElseThrow().step()
                instanceof AuthoringStep.CreateAwaitingPrefix);
    }

    @Test
    void malformedPrefixReportsErrorAndRetriesSameStep(@TempDir final Path dir) throws Exception {
        final Fixture fixture = buildFixture(dir);
        final UUID playerId = UUID.randomUUID();
        final Player player = mockPlayer(playerId);

        fixture.flow().startCreate(player, CosmeticKind.TAG);
        fixture.flow().handleInput(player, "vip");
        fixture.flow().handleInput(player, "<gold><bold>unclosed");

        assertTrue(fixture.sessionStore().find(playerId).orElseThrow().step()
                instanceof AuthoringStep.CreateAwaitingPrefix);

        fixture.flow().handleInput(player, "<gold>[VIP]</gold>");
        assertTrue(fixture.sessionStore().find(playerId).orElseThrow().step()
                instanceof AuthoringStep.CreateAwaitingPermission);
    }

    @Test
    void nonNumericWeightRetriesSameStep(@TempDir final Path dir) throws Exception {
        final Fixture fixture = buildFixture(dir);
        final UUID playerId = UUID.randomUUID();
        final Player player = mockPlayer(playerId);

        fixture.flow().startCreate(player, CosmeticKind.TAG);
        fixture.flow().handleInput(player, "vip");
        fixture.flow().handleInput(player, "<gold>[VIP]</gold>");
        fixture.flow().handleInput(player, "stags.tag.vip");
        fixture.flow().handleInput(player, "not-a-number");

        assertTrue(fixture.sessionStore().find(playerId).orElseThrow().step()
                instanceof AuthoringStep.CreateAwaitingWeight);
    }

    @Test
    void negativeWeightRetriesSameStepWithoutBreakingTheSession(@TempDir final Path dir) throws Exception {
        final Fixture fixture = buildFixture(dir);
        final UUID playerId = UUID.randomUUID();
        final Player player = mockPlayer(playerId);

        fixture.flow().startCreate(player, CosmeticKind.TAG);
        fixture.flow().handleInput(player, "vip");
        fixture.flow().handleInput(player, "<gold>[VIP]</gold>");
        fixture.flow().handleInput(player, "stags.tag.vip");
        fixture.flow().handleInput(player, "-1");

        assertTrue(fixture.sessionStore().find(playerId).orElseThrow().step()
                instanceof AuthoringStep.CreateAwaitingWeight);

        fixture.flow().handleInput(player, "100");
        fixture.flow().handleInput(player, "confirm");

        assertTrue(fixture.catalogueService().catalogue().contains(CosmeticKind.TAG, new CosmeticId("vip")));
    }

    @Test
    void editFlowSkipsIdentifierAndUpdatesPrefix(@TempDir final Path dir) throws Exception {
        final Fixture fixture = buildFixture(dir);
        final var created = fixture.catalogueService()
                .create(CosmeticKind.TAG, "vip", "<gold>[VIP]</gold>", "stags.tag.vip", 100).get();
        final var cosmetic = ((io.github.viniciussambinello.stags.domain.catalogue.ValidationResult.Accepted) created).cosmetic();

        final UUID playerId = UUID.randomUUID();
        final Player player = mockPlayer(playerId);
        fixture.flow().startEdit(player, cosmetic);
        fixture.flow().handleInput(player, "<red>[VIP+]</red>");
        fixture.flow().handleInput(player, "skip");
        fixture.flow().handleInput(player, "200");
        fixture.flow().handleInput(player, "confirm");

        final var updated = fixture.catalogueService().catalogue().find(CosmeticKind.TAG, new CosmeticId("vip")).orElseThrow();
        assertEquals("<red>[VIP+]</red>", updated.prefix().raw());
        assertEquals(200, updated.weight().value());
        assertEquals("stags.tag.vip", updated.permission().value());
    }

    @Test
    void editTargetDeletedMidFlowIsRefused(@TempDir final Path dir) throws Exception {
        final Fixture fixture = buildFixture(dir);
        final var created = fixture.catalogueService()
                .create(CosmeticKind.TAG, "vip", "<gold>[VIP]</gold>", "stags.tag.vip", 100).get();
        final var cosmetic = ((io.github.viniciussambinello.stags.domain.catalogue.ValidationResult.Accepted) created).cosmetic();

        final UUID playerId = UUID.randomUUID();
        final Player player = mockPlayer(playerId);
        fixture.flow().startEdit(player, cosmetic);
        fixture.flow().handleInput(player, "<red>[VIP+]</red>");
        fixture.flow().handleInput(player, "skip");
        fixture.flow().handleInput(player, "200");

        fixture.catalogueService().delete(CosmeticKind.TAG, new CosmeticId("vip")).get();

        fixture.flow().handleInput(player, "confirm");

        assertTrue(fixture.sessionStore().find(playerId).isEmpty());
        assertTrue(fixture.catalogueService().catalogue().find(CosmeticKind.TAG, new CosmeticId("vip")).isEmpty());
    }

    @Test
    void deletingAnActiveTagNotifiesAndRefreshesTheAffectedOnlinePlayer(@TempDir final Path dir) throws Exception {
        final Fixture fixture = buildFixture(dir);
        fixture.catalogueService().create(CosmeticKind.TAG, "vip", "<gold>[VIP]</gold>", "stags.tag.vip", 100).get();

        final UUID affectedId = UUID.randomUUID();
        final Player affected = mockPlayer(affectedId);
        Mockito.doReturn(Set.of(affected)).when(fixture.server()).getOnlinePlayers();
        Mockito.when(fixture.server().getPlayer(affectedId)).thenReturn(affected);

        fixture.playerCosmeticService().load(affectedId).get();
        fixture.playerCosmeticService().updateSelection(
                affectedId, CosmeticKind.TAG, new io.github.viniciussambinello.stags.domain.player.Selection.Active(new CosmeticId("vip"))).get();

        final UUID deleterId = UUID.randomUUID();
        final Player deleter = mockPlayer(deleterId);
        fixture.flow().startDelete(deleter, CosmeticKind.TAG, new CosmeticId("vip"));
        fixture.flow().handleInput(deleter, "confirm");

        Mockito.verify(affected, Mockito.atLeastOnce()).sendMessage(Mockito.any(net.kyori.adventure.text.Component.class));
    }

    @Test
    void deleteFlowRemovesTheCosmeticOnConfirm(@TempDir final Path dir) throws Exception {
        final Fixture fixture = buildFixture(dir);
        fixture.catalogueService().create(CosmeticKind.TAG, "vip", "<gold>[VIP]</gold>", "stags.tag.vip", 100).get();

        final UUID playerId = UUID.randomUUID();
        final Player player = mockPlayer(playerId);
        fixture.flow().startDelete(player, CosmeticKind.TAG, new CosmeticId("vip"));
        fixture.flow().handleInput(player, "confirm");

        assertTrue(fixture.catalogueService().catalogue().all(CosmeticKind.TAG).isEmpty());
    }

    @Test
    void deleteFlowNotConfirmedLeavesCosmeticUnchanged(@TempDir final Path dir) throws Exception {
        final Fixture fixture = buildFixture(dir);
        fixture.catalogueService().create(CosmeticKind.TAG, "vip", "<gold>[VIP]</gold>", "stags.tag.vip", 100).get();

        final UUID playerId = UUID.randomUUID();
        final Player player = mockPlayer(playerId);
        fixture.flow().startDelete(player, CosmeticKind.TAG, new CosmeticId("vip"));
        fixture.flow().handleInput(player, "nevermind");

        assertTrue(fixture.catalogueService().catalogue().contains(CosmeticKind.TAG, new CosmeticId("vip")));
    }

    @Test
    void sessionExpiresAfterTimeout(@TempDir final Path dir) throws Exception {
        final Fixture fixture = buildFixture(dir);
        final UUID playerId = UUID.randomUUID();
        final Player player = mockPlayer(playerId);

        fixture.flow().startCreate(player, CosmeticKind.TAG);
        assertTrue(fixture.sessionStore().find(playerId).isPresent());

        fixture.clock().advance(Duration.ofSeconds(200));
        fixture.flow().sweepExpired();

        assertTrue(fixture.sessionStore().find(playerId).isEmpty());
    }

    @Test
    void concurrentConfirmRaceOnSameIdentifierYieldsExactlyOneWinner(@TempDir final Path dir) throws Exception {
        final Fixture fixture = buildFixture(dir);
        final UUID playerOneId = UUID.randomUUID();
        final UUID playerTwoId = UUID.randomUUID();
        final Player playerOne = mockPlayer(playerOneId);
        final Player playerTwo = mockPlayer(playerTwoId);

        fixture.flow().startCreate(playerOne, CosmeticKind.TAG);
        fixture.flow().handleInput(playerOne, "vip");
        fixture.flow().handleInput(playerOne, "<gold>[VIP]</gold>");
        fixture.flow().handleInput(playerOne, "skip");
        fixture.flow().handleInput(playerOne, "100");

        fixture.flow().startCreate(playerTwo, CosmeticKind.TAG);
        fixture.flow().handleInput(playerTwo, "vip");
        fixture.flow().handleInput(playerTwo, "<red>[VIP]</red>");
        fixture.flow().handleInput(playerTwo, "skip");
        fixture.flow().handleInput(playerTwo, "50");

        fixture.flow().handleInput(playerOne, "confirm");
        fixture.flow().handleInput(playerTwo, "confirm");

        assertEquals(1, fixture.catalogueService().catalogue().all(CosmeticKind.TAG).size());
    }
}
