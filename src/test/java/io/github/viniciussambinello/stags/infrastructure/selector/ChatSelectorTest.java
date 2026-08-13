package io.github.viniciussambinello.stags.infrastructure.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.github.viniciussambinello.stags.application.service.CatalogueService;
import io.github.viniciussambinello.stags.application.service.PlayerCosmeticService;
import io.github.viniciussambinello.stags.application.service.SelectorCooldownService;
import io.github.viniciussambinello.stags.application.service.SelectorService;
import io.github.viniciussambinello.stags.application.usecase.ClearCosmetic;
import io.github.viniciussambinello.stags.application.usecase.SelectCosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.cosmetic.PermissionNode;
import io.github.viniciussambinello.stags.domain.cosmetic.Prefix;
import io.github.viniciussambinello.stags.domain.cosmetic.Weight;
import io.github.viniciussambinello.stags.domain.player.PlayerCosmetics;
import io.github.viniciussambinello.stags.domain.player.Selection;
import io.github.viniciussambinello.stags.infrastructure.concurrent.MainThreadDispatcher;
import io.github.viniciussambinello.stags.infrastructure.config.ConfigService;
import io.github.viniciussambinello.stags.testkit.InMemoryCosmeticRepository;
import io.github.viniciussambinello.stags.testkit.InMemorySelectionRepository;
import io.github.viniciussambinello.stags.testkit.RecordingCosmeticRenderer;
import io.github.viniciussambinello.stags.testkit.StubPermissionOracle;
import net.kyori.adventure.text.Component;

final class ChatSelectorTest {

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
            SelectorService selectorService, SelectCosmetic selectCosmetic, ClearCosmetic clearCosmetic,
            PlayerCosmeticService playerCosmeticService, RecordingCosmeticRenderer renderer) {
    }

    private Fixture buildFixture(final UUID playerId, final boolean grantVip) throws Exception {
        final InMemoryCosmeticRepository cosmeticRepository = new InMemoryCosmeticRepository();
        cosmeticRepository.insert(new Cosmetic(
                CosmeticKind.TAG, new CosmeticId("vip"), Prefix.parse("<gold>[VIP]</gold>"),
                new PermissionNode("stags.tag.vip"), new Weight(100))).get();
        final CatalogueService catalogueService = new CatalogueService(cosmeticRepository);
        catalogueService.loadInitial().get();

        final PlayerCosmeticService playerCosmeticService = new PlayerCosmeticService(new InMemorySelectionRepository());
        playerCosmeticService.load(playerId).get();

        final StubPermissionOracle permissions = new StubPermissionOracle();
        if (grantVip) {
            permissions.grant(playerId, "stags.tag.vip");
        }
        final SelectorService selectorService = new SelectorService(catalogueService, playerCosmeticService, permissions);
        final RecordingCosmeticRenderer renderer = new RecordingCosmeticRenderer();
        final SelectCosmetic selectCosmetic = new SelectCosmetic(catalogueService, playerCosmeticService, permissions, renderer);
        final ClearCosmetic clearCosmetic = new ClearCosmetic(playerCosmeticService, renderer);
        return new Fixture(selectorService, selectCosmetic, clearCosmetic, playerCosmeticService, renderer);
    }

    @Test
    void ownedEntryIsClickableAndSelectsOnClick(@TempDir final Path dir) throws Exception {
        final UUID playerId = UUID.randomUUID();
        final Fixture fixture = buildFixture(playerId, true);

        final ConfigService configService = ConfigService.initial(dir, Logger.getLogger("test"));
        final SelectorCooldownService cooldownService = new SelectorCooldownService(Clock.systemUTC());
        final ChatSelector chatSelector = new ChatSelector(
                configService, fixture.selectorService(), fixture.selectCosmetic(), fixture.clearCosmetic(),
                cooldownService, immediateDispatcher(), Clock.systemUTC());

        final Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(playerId);
        Mockito.when(player.isOnline()).thenReturn(true);

        chatSelector.open(player, CosmeticKind.TAG);

        final ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        Mockito.verify(player, Mockito.atLeastOnce()).sendMessage(captor.capture());
        assertNotNull(captor.getAllValues().get(0).clickEvent());

        final SelectorService.Entry entry = fixture.selectorService().entries(playerId, CosmeticKind.TAG, false).get(0);
        chatSelector.onEntryClicked(player, CosmeticKind.TAG, entry);

        assertEquals(1, fixture.renderer().refreshed().size());
        final PlayerCosmetics loaded = fixture.playerCosmeticService().cached(playerId).orElseThrow();
        assertEquals(new Selection.Active(new CosmeticId("vip")), loaded.tagSelection());
    }

    @Test
    void lockedEntryHasNoClickEventAndClickingItIsRefused(@TempDir final Path dir) throws Exception {
        final UUID playerId = UUID.randomUUID();
        final Fixture fixture = buildFixture(playerId, false);

        final ConfigService configService = ConfigService.initial(dir, Logger.getLogger("test"));
        final SelectorCooldownService cooldownService = new SelectorCooldownService(Clock.systemUTC());
        final ChatSelector chatSelector = new ChatSelector(
                configService, fixture.selectorService(), fixture.selectCosmetic(), fixture.clearCosmetic(),
                cooldownService, immediateDispatcher(), Clock.systemUTC());

        final Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(playerId);
        Mockito.when(player.isOnline()).thenReturn(true);

        chatSelector.open(player, CosmeticKind.TAG);

        final ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        Mockito.verify(player, Mockito.atLeastOnce()).sendMessage(captor.capture());
        assertNull(captor.getAllValues().get(0).clickEvent());

        final SelectorService.Entry entry = fixture.selectorService().entries(playerId, CosmeticKind.TAG, false).get(0);
        chatSelector.onEntryClicked(player, CosmeticKind.TAG, entry);

        assertEquals(0, fixture.renderer().refreshed().size());
    }

    @Test
    void clickAfterExpiryShowsExpiredMessageInsteadOfSelecting(@TempDir final Path dir) throws Exception {
        final UUID playerId = UUID.randomUUID();
        final Fixture fixture = buildFixture(playerId, true);

        final ConfigService configService = ConfigService.initial(dir, Logger.getLogger("test"));
        final SelectorCooldownService cooldownService = new SelectorCooldownService(Clock.systemUTC());
        final MutableClock clock = new MutableClock(Instant.EPOCH);
        final ChatSelector chatSelector = new ChatSelector(
                configService, fixture.selectorService(), fixture.selectCosmetic(), fixture.clearCosmetic(),
                cooldownService, immediateDispatcher(), clock);

        final Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(playerId);
        Mockito.when(player.isOnline()).thenReturn(true);

        chatSelector.open(player, CosmeticKind.TAG);
        final SelectorService.Entry entry = fixture.selectorService().entries(playerId, CosmeticKind.TAG, false).get(0);

        clock.advance(Duration.ofMinutes(5));
        chatSelector.onEntryClicked(player, CosmeticKind.TAG, entry);

        assertEquals(0, fixture.renderer().refreshed().size());
        final PlayerCosmetics loaded = fixture.playerCosmeticService().cached(playerId).orElseThrow();
        assertEquals(Selection.UNSET, loaded.tagSelection());
    }
}
