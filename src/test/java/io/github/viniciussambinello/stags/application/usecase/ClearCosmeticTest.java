package io.github.viniciussambinello.stags.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.viniciussambinello.stags.application.service.PlayerCosmeticService;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.player.PlayerCosmetics;
import io.github.viniciussambinello.stags.domain.player.Selection;
import io.github.viniciussambinello.stags.testkit.InMemorySelectionRepository;
import io.github.viniciussambinello.stags.testkit.RecordingCosmeticRenderer;

final class ClearCosmeticTest {

    private final UUID playerId = UUID.randomUUID();

    @Test
    void clearsAnActiveSelectionAndRefreshesRendering() throws Exception {
        final InMemorySelectionRepository repository = new InMemorySelectionRepository();
        repository.put(new PlayerCosmetics(playerId, new Selection.Active(new CosmeticId("vip")), Selection.UNSET));
        final PlayerCosmeticService playerService = new PlayerCosmeticService(repository);
        playerService.load(playerId).get();
        final RecordingCosmeticRenderer renderer = new RecordingCosmeticRenderer();

        final ClearCosmetic useCase = new ClearCosmetic(playerService, renderer);
        final ClearCosmetic.Result result = useCase.execute(playerId, CosmeticKind.TAG).get();

        assertInstanceOf(ClearCosmetic.Result.Cleared.class, result);
        assertEquals(Selection.CLEARED, playerService.cached(playerId).orElseThrow().tagSelection());
        assertEquals(1, renderer.refreshed().size());
    }

    @Test
    void storageFailureIsReported() throws Exception {
        final InMemorySelectionRepository repository = new InMemorySelectionRepository();
        repository.put(new PlayerCosmetics(playerId, new Selection.Active(new CosmeticId("vip")), Selection.UNSET));
        final PlayerCosmeticService playerService = new PlayerCosmeticService(repository);
        playerService.load(playerId).get();
        final RecordingCosmeticRenderer renderer = new RecordingCosmeticRenderer();

        repository.failNextSave();
        final ClearCosmetic useCase = new ClearCosmetic(playerService, renderer);
        final ClearCosmetic.Result result = useCase.execute(playerId, CosmeticKind.TAG).get();

        assertInstanceOf(ClearCosmetic.Result.StorageFailure.class, result);
        assertEquals(0, renderer.refreshed().size());
    }
}
