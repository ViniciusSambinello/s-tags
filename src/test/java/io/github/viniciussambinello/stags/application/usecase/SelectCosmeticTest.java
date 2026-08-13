package io.github.viniciussambinello.stags.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import io.github.viniciussambinello.stags.application.service.CatalogueService;
import io.github.viniciussambinello.stags.application.service.PlayerCosmeticService;
import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.cosmetic.PermissionNode;
import io.github.viniciussambinello.stags.domain.cosmetic.Prefix;
import io.github.viniciussambinello.stags.domain.cosmetic.Weight;
import io.github.viniciussambinello.stags.domain.player.PlayerCosmetics;
import io.github.viniciussambinello.stags.testkit.InMemoryCosmeticRepository;
import io.github.viniciussambinello.stags.testkit.InMemorySelectionRepository;
import io.github.viniciussambinello.stags.testkit.RecordingCosmeticRenderer;
import io.github.viniciussambinello.stags.testkit.StubPermissionOracle;

final class SelectCosmeticTest {

    private final UUID playerId = UUID.randomUUID();

    private CatalogueService buildCatalogueWithVipTag() throws ExecutionException, InterruptedException {
        final InMemoryCosmeticRepository repository = new InMemoryCosmeticRepository();
        repository.insert(new Cosmetic(
                CosmeticKind.TAG, new CosmeticId("vip"), Prefix.parse("<gold>[VIP]</gold>"),
                new PermissionNode("stags.tag.vip"), new Weight(100))).get();
        final CatalogueService service = new CatalogueService(repository);
        service.loadInitial().get();
        return service;
    }

    @Test
    void appliesAnOwnedCosmetic() throws Exception {
        final CatalogueService catalogueService = buildCatalogueWithVipTag();
        final PlayerCosmeticService playerService = new PlayerCosmeticService(new InMemorySelectionRepository());
        playerService.load(playerId).get();
        final StubPermissionOracle permissions = new StubPermissionOracle().grant(playerId, "stags.tag.vip");
        final RecordingCosmeticRenderer renderer = new RecordingCosmeticRenderer();

        final SelectCosmetic useCase = new SelectCosmetic(catalogueService, playerService, permissions, renderer);
        final SelectCosmetic.Result result = useCase.execute(playerId, CosmeticKind.TAG, new CosmeticId("vip")).get();

        assertInstanceOf(SelectCosmetic.Result.Applied.class, result);
        assertEquals(1, renderer.refreshed().size());
    }

    @Test
    void refusesAnUnownedCosmetic() throws Exception {
        final CatalogueService catalogueService = buildCatalogueWithVipTag();
        final PlayerCosmeticService playerService = new PlayerCosmeticService(new InMemorySelectionRepository());
        playerService.load(playerId).get();
        final StubPermissionOracle permissions = new StubPermissionOracle();
        final RecordingCosmeticRenderer renderer = new RecordingCosmeticRenderer();

        final SelectCosmetic useCase = new SelectCosmetic(catalogueService, playerService, permissions, renderer);
        final SelectCosmetic.Result result = useCase.execute(playerId, CosmeticKind.TAG, new CosmeticId("vip")).get();

        assertInstanceOf(SelectCosmetic.Result.NotOwned.class, result);
        assertEquals(0, renderer.refreshed().size());
    }

    @Test
    void refusesAnUnknownCosmetic() throws Exception {
        final CatalogueService catalogueService = buildCatalogueWithVipTag();
        final PlayerCosmeticService playerService = new PlayerCosmeticService(new InMemorySelectionRepository());
        playerService.load(playerId).get();
        final StubPermissionOracle permissions = new StubPermissionOracle();
        final RecordingCosmeticRenderer renderer = new RecordingCosmeticRenderer();

        final SelectCosmetic useCase = new SelectCosmetic(catalogueService, playerService, permissions, renderer);
        final SelectCosmetic.Result result = useCase.execute(playerId, CosmeticKind.TAG, new CosmeticId("missing")).get();

        assertInstanceOf(SelectCosmetic.Result.UnknownCosmetic.class, result);
    }

    @Test
    void reportsStorageFailureAndLeavesCacheConsistent() throws Exception {
        final CatalogueService catalogueService = buildCatalogueWithVipTag();
        final InMemorySelectionRepository selectionRepository = new InMemorySelectionRepository();
        final PlayerCosmeticService playerService = new PlayerCosmeticService(selectionRepository);
        playerService.load(playerId).get();
        final StubPermissionOracle permissions = new StubPermissionOracle().grant(playerId, "stags.tag.vip");
        final RecordingCosmeticRenderer renderer = new RecordingCosmeticRenderer();

        selectionRepository.failNextSave();
        final SelectCosmetic useCase = new SelectCosmetic(catalogueService, playerService, permissions, renderer);
        final SelectCosmetic.Result result = useCase.execute(playerId, CosmeticKind.TAG, new CosmeticId("vip")).get();

        assertInstanceOf(SelectCosmetic.Result.StorageFailure.class, result);
        final PlayerCosmetics cached = playerService.cached(playerId).orElseThrow();
        assertEquals(io.github.viniciussambinello.stags.domain.player.Selection.UNSET, cached.tagSelection());
        assertEquals(0, renderer.refreshed().size());
    }
}
