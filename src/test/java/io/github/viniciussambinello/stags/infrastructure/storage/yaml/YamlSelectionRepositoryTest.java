package io.github.viniciussambinello.stags.infrastructure.storage.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import org.bukkit.Server;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.player.PlayerCosmetics;
import io.github.viniciussambinello.stags.domain.player.Selection;
import io.github.viniciussambinello.stags.infrastructure.concurrent.MainThreadGuard;
import io.github.viniciussambinello.stags.infrastructure.concurrent.StorageExecutor;

final class YamlSelectionRepositoryTest {

    private MainThreadGuard offMainThreadGuard() {
        final Server server = Mockito.mock(Server.class);
        Mockito.when(server.isPrimaryThread()).thenReturn(false);
        return new MainThreadGuard(server);
    }

    @Test
    void neverSelectedPlayerLoadsAsUnset(@TempDir final Path dir) throws Exception {
        try (StorageExecutor executor = new StorageExecutor();
                YamlSelectionRepository repository = new YamlSelectionRepository(
                        dir.resolve("selections.yml"), executor, offMainThreadGuard(), Duration.ofMillis(50))) {
            final PlayerCosmetics loaded = repository.load(UUID.randomUUID()).get();
            assertEquals(Selection.UNSET, loaded.tagSelection());
            assertEquals(Selection.UNSET, loaded.titleSelection());
        }
    }

    @Test
    void threeSelectionStatesSurviveARestart(@TempDir final Path dir) throws Exception {
        final Path file = dir.resolve("selections.yml");
        final UUID active = UUID.randomUUID();
        final UUID cleared = UUID.randomUUID();

        try (StorageExecutor executor = new StorageExecutor()) {
            try (YamlSelectionRepository first = new YamlSelectionRepository(file, executor, offMainThreadGuard(), Duration.ofMillis(50))) {
                first.save(active, CosmeticKind.TAG, new Selection.Active(new CosmeticId("vip"))).get();
                first.save(cleared, CosmeticKind.TAG, Selection.CLEARED).get();
            }
            try (YamlSelectionRepository second = new YamlSelectionRepository(file, executor, offMainThreadGuard(), Duration.ofMillis(50))) {
                final PlayerCosmetics activeLoaded = second.load(active).get();
                assertEquals(new Selection.Active(new CosmeticId("vip")), activeLoaded.tagSelection());

                final PlayerCosmetics clearedLoaded = second.load(cleared).get();
                assertEquals(Selection.CLEARED, clearedLoaded.tagSelection());

                final PlayerCosmetics neverStored = second.load(UUID.randomUUID()).get();
                assertEquals(Selection.UNSET, neverStored.tagSelection());
            }
        }
    }
}
