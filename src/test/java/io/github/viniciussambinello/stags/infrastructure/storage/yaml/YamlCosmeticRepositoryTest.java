package io.github.viniciussambinello.stags.infrastructure.storage.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.bukkit.Server;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import io.github.viniciussambinello.stags.application.port.CosmeticRepository;
import io.github.viniciussambinello.stags.domain.catalogue.Catalogue;
import io.github.viniciussambinello.stags.domain.cosmetic.Cosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.cosmetic.PermissionNode;
import io.github.viniciussambinello.stags.domain.cosmetic.Prefix;
import io.github.viniciussambinello.stags.domain.cosmetic.Weight;
import io.github.viniciussambinello.stags.infrastructure.concurrent.MainThreadGuard;
import io.github.viniciussambinello.stags.infrastructure.concurrent.StorageExecutor;

final class YamlCosmeticRepositoryTest {

    private MainThreadGuard offMainThreadGuard() {
        final Server server = Mockito.mock(Server.class);
        Mockito.when(server.isPrimaryThread()).thenReturn(false);
        return new MainThreadGuard(server);
    }

    private static Cosmetic vip() {
        return new Cosmetic(
                CosmeticKind.TAG, new CosmeticId("vip"), Prefix.parse("<gold>[VIP]</gold>"),
                new PermissionNode("stags.tag.vip"), new Weight(100));
    }

    @Test
    void insertRejectsDuplicateIdentifier(@TempDir final Path dir) throws Exception {
        try (StorageExecutor executor = new StorageExecutor();
                YamlCosmeticRepository repository = new YamlCosmeticRepository(
                        dir.resolve("cosmetics.yml"), executor, offMainThreadGuard(), Duration.ofMillis(50))) {
            assertEquals(CosmeticRepository.InsertOutcome.CREATED, repository.insert(vip()).get());
            assertEquals(CosmeticRepository.InsertOutcome.DUPLICATE, repository.insert(vip()).get());
        }
    }

    @Test
    void manyChangesWithinOneIntervalProduceExactlyOneWrite(@TempDir final Path dir) throws Exception {
        final Path file = dir.resolve("cosmetics.yml");
        try (StorageExecutor executor = new StorageExecutor();
                YamlCosmeticRepository repository = new YamlCosmeticRepository(
                        file, executor, offMainThreadGuard(), Duration.ofSeconds(2))) {
            for (int index = 0; index < 20; index++) {
                repository.insert(new Cosmetic(
                        CosmeticKind.TAG, new CosmeticId("tag" + index), Prefix.parse("<gray>x</gray>"),
                        PermissionNode.NONE, new Weight(index))).get();
            }
            assertTrue(Files.notExists(file), "file should not be written before the debounce interval elapses");
        }
        assertTrue(Files.exists(file), "file should be written on close via the final flush");
        final long tagCount = YamlFiles.loadOrEmpty(file).getConfigurationSection("tag").getKeys(false).size();
        assertEquals(20, tagCount);
    }

    @Test
    void restartingWithANewRepositoryInstanceRestoresState(@TempDir final Path dir) throws Exception {
        final Path file = dir.resolve("cosmetics.yml");
        try (StorageExecutor executor = new StorageExecutor()) {
            try (YamlCosmeticRepository first = new YamlCosmeticRepository(file, executor, offMainThreadGuard(), Duration.ofMillis(50))) {
                first.insert(vip()).get();
            }
            try (YamlCosmeticRepository second = new YamlCosmeticRepository(file, executor, offMainThreadGuard(), Duration.ofMillis(50))) {
                final Catalogue catalogue = second.loadAll().get();
                final Cosmetic restored = catalogue.find(CosmeticKind.TAG, new CosmeticId("vip")).orElseThrow();
                assertEquals("stags.tag.vip", restored.permission().value());
                assertEquals(100, restored.weight().value());
                assertEquals("<gold>[VIP]</gold>", restored.prefix().raw());
            }
        }
    }
}
