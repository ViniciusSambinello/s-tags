package io.github.viniciussambinello.stags.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.player.PlayerCosmetics;
import io.github.viniciussambinello.stags.domain.player.Selection;
import io.github.viniciussambinello.stags.testkit.InMemorySelectionRepository;

final class PlayerCosmeticServiceTest {

    private final UUID playerId = UUID.randomUUID();

    @Test
    void loadCachesAfterFirstCall() throws Exception {
        final InMemorySelectionRepository repository = new InMemorySelectionRepository();
        repository.put(new PlayerCosmetics(playerId, new Selection.Active(new CosmeticId("vip")), Selection.UNSET));
        final PlayerCosmeticService service = new PlayerCosmeticService(repository);

        final PlayerCosmetics first = service.load(playerId).get();
        assertEquals(new Selection.Active(new CosmeticId("vip")), first.tagSelection());
        assertTrue(service.cached(playerId).isPresent());
    }

    @Test
    void redundantSelectionDoesNotIssueAWrite() throws Exception {
        final InMemorySelectionRepository repository = new InMemorySelectionRepository();
        repository.put(new PlayerCosmetics(playerId, new Selection.Active(new CosmeticId("vip")), Selection.UNSET));
        final PlayerCosmeticService service = new PlayerCosmeticService(repository);
        service.load(playerId).get();

        repository.failNextSave();
        final boolean success = service.updateSelection(playerId, CosmeticKind.TAG, new Selection.Active(new CosmeticId("vip"))).get();
        assertTrue(success);
    }

    @Test
    void failedWriteRollsBackCacheToPreviousValue() throws Exception {
        final InMemorySelectionRepository repository = new InMemorySelectionRepository();
        repository.put(PlayerCosmetics.unset(playerId));
        final PlayerCosmeticService service = new PlayerCosmeticService(repository);
        service.load(playerId).get();

        repository.failNextSave();
        final boolean success = service.updateSelection(playerId, CosmeticKind.TAG, new Selection.Active(new CosmeticId("vip"))).get();

        assertFalse(success);
        assertEquals(Selection.UNSET, service.cached(playerId).orElseThrow().tagSelection());
    }

    @Test
    void releaseRemovesFromCache() throws Exception {
        final InMemorySelectionRepository repository = new InMemorySelectionRepository();
        final PlayerCosmeticService service = new PlayerCosmeticService(repository);
        service.load(playerId).get();
        assertTrue(service.cached(playerId).isPresent());

        service.release(playerId);
        assertTrue(service.cached(playerId).isEmpty());
    }
}
