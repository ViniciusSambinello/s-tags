package io.github.viniciussambinello.stags.infrastructure.storage.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;

import org.bukkit.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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
import io.github.viniciussambinello.stags.infrastructure.config.MySqlConfig;

@Tag("mysql-integration")
final class MySqlCosmeticRepositoryTest {

    private String tablePrefix;
    private HikariConnectionProvider connectionProvider;
    private StorageExecutor executor;

    private MainThreadGuard offMainThreadGuard() {
        final Server server = Mockito.mock(Server.class);
        Mockito.when(server.isPrimaryThread()).thenReturn(false);
        return new MainThreadGuard(server);
    }

    private MySqlCosmeticRepository freshRepository() throws SQLException {
        tablePrefix = MySqlTestSupport.freshTablePrefix();
        final MySqlConfig config = MySqlTestSupport.config(tablePrefix);
        connectionProvider = new HikariConnectionProvider(config);
        new SchemaMigrator(connectionProvider, tablePrefix).migrate();
        executor = new StorageExecutor();
        return new MySqlCosmeticRepository(connectionProvider, executor, offMainThreadGuard(), tablePrefix);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (executor != null) {
            executor.close();
        }
        if (connectionProvider != null) {
            MySqlTestSupport.dropTables(connectionProvider, tablePrefix);
            connectionProvider.close();
        }
    }

    private static Cosmetic vip() {
        return new Cosmetic(
                CosmeticKind.TAG, new CosmeticId("vip"), Prefix.parse("<gold>[VIP]</gold>"),
                new PermissionNode("stags.tag.vip"), new Weight(100));
    }

    @Test
    void insertThenLoadAllRoundTrips() throws Exception {
        final MySqlCosmeticRepository repository = freshRepository();
        assertEquals(CosmeticRepository.InsertOutcome.CREATED, repository.insert(vip()).get());

        final Catalogue catalogue = repository.loadAll().get();
        final Cosmetic loaded = catalogue.find(CosmeticKind.TAG, new CosmeticId("vip")).orElseThrow();
        assertEquals("<gold>[VIP]</gold>", loaded.prefix().raw());
        assertEquals("stags.tag.vip", loaded.permission().value());
        assertEquals(100, loaded.weight().value());
    }

    @Test
    void insertDuplicateReturnsDuplicateOutcome() throws Exception {
        final MySqlCosmeticRepository repository = freshRepository();
        repository.insert(vip()).get();
        assertEquals(CosmeticRepository.InsertOutcome.DUPLICATE, repository.insert(vip()).get());
    }

    @Test
    void sameIdentifierAllowedAcrossDifferentKinds() throws Exception {
        final MySqlCosmeticRepository repository = freshRepository();
        repository.insert(vip()).get();
        final Cosmetic title = new Cosmetic(
                CosmeticKind.TITLE, new CosmeticId("vip"), Prefix.parse("<gold>Champion</gold>"),
                new PermissionNode("stags.title.vip"), new Weight(500));
        assertEquals(CosmeticRepository.InsertOutcome.CREATED, repository.insert(title).get());
    }

    @Test
    void updateChangesPrefixPermissionAndWeight() throws Exception {
        final MySqlCosmeticRepository repository = freshRepository();
        repository.insert(vip()).get();

        final Cosmetic updated = vip().withPrefix(Prefix.parse("<red>[VIP+]</red>")).withWeight(new Weight(200));
        repository.update(updated).get();

        final Catalogue catalogue = repository.loadAll().get();
        final Cosmetic loaded = catalogue.find(CosmeticKind.TAG, new CosmeticId("vip")).orElseThrow();
        assertEquals("<red>[VIP+]</red>", loaded.prefix().raw());
        assertEquals(200, loaded.weight().value());
    }

    @Test
    void deleteRemovesTheRow() throws Exception {
        final MySqlCosmeticRepository repository = freshRepository();
        repository.insert(vip()).get();
        repository.delete(CosmeticKind.TAG, new CosmeticId("vip")).get();

        final Catalogue catalogue = repository.loadAll().get();
        assertTrue(catalogue.all(CosmeticKind.TAG).isEmpty());
    }
}
