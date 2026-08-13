package io.github.viniciussambinello.stags.infrastructure.storage.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.player.PlayerCosmetics;
import io.github.viniciussambinello.stags.domain.player.Selection;
import io.github.viniciussambinello.stags.infrastructure.concurrent.MainThreadGuard;
import io.github.viniciussambinello.stags.infrastructure.concurrent.StorageExecutor;
import io.github.viniciussambinello.stags.infrastructure.config.MySqlConfig;

@Tag("mysql-integration")
final class MySqlSelectionRepositoryTest {

    private String tablePrefix;
    private HikariConnectionProvider connectionProvider;
    private StorageExecutor executor;

    private MainThreadGuard offMainThreadGuard() {
        final Server server = Mockito.mock(Server.class);
        Mockito.when(server.isPrimaryThread()).thenReturn(false);
        return new MainThreadGuard(server);
    }

    private MySqlSelectionRepository freshRepository() throws SQLException {
        tablePrefix = MySqlTestSupport.freshTablePrefix();
        final MySqlConfig config = MySqlTestSupport.config(tablePrefix);
        connectionProvider = new HikariConnectionProvider(config);
        new SchemaMigrator(connectionProvider, tablePrefix).migrate();
        executor = new StorageExecutor();
        return new MySqlSelectionRepository(connectionProvider, executor, offMainThreadGuard(), tablePrefix);
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

    @Test
    void neverSelectedPlayerLoadsAsUnset() throws Exception {
        final MySqlSelectionRepository repository = freshRepository();
        final PlayerCosmetics loaded = repository.load(UUID.randomUUID()).get();
        assertEquals(Selection.UNSET, loaded.tagSelection());
        assertEquals(Selection.UNSET, loaded.titleSelection());
    }

    @Test
    void threeStatesRoundTripThroughSave() throws Exception {
        final MySqlSelectionRepository repository = freshRepository();
        final UUID player = UUID.randomUUID();

        repository.save(player, CosmeticKind.TAG, new Selection.Active(new CosmeticId("vip"))).get();
        repository.save(player, CosmeticKind.TITLE, Selection.CLEARED).get();

        final PlayerCosmetics loaded = repository.load(player).get();
        assertEquals(new Selection.Active(new CosmeticId("vip")), loaded.tagSelection());
        assertEquals(Selection.CLEARED, loaded.titleSelection());
    }

    @Test
    void savingTwiceUpsertsRatherThanDuplicating() throws Exception {
        final MySqlSelectionRepository repository = freshRepository();
        final UUID player = UUID.randomUUID();

        repository.save(player, CosmeticKind.TAG, new Selection.Active(new CosmeticId("vip"))).get();
        repository.save(player, CosmeticKind.TAG, new Selection.Active(new CosmeticId("member"))).get();

        final PlayerCosmetics loaded = repository.load(player).get();
        assertEquals(new Selection.Active(new CosmeticId("member")), loaded.tagSelection());

        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*) AS total FROM " + tablePrefix + "player_selection WHERE player_uuid = ? AND kind = ?")) {
            statement.setBytes(1, UuidBinary.toBytes(player));
            statement.setString(2, "TAG");
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                assertEquals(1, resultSet.getInt("total"));
            }
        }
    }

    @Test
    void lookupUsesThePrimaryKeyIndex() throws Exception {
        final MySqlSelectionRepository repository = freshRepository();
        final UUID player = UUID.randomUUID();
        repository.save(player, CosmeticKind.TAG, new Selection.Active(new CosmeticId("vip"))).get();

        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "EXPLAIN SELECT kind, cosmetic_id, cleared FROM " + tablePrefix + "player_selection WHERE player_uuid = ?")) {
            statement.setBytes(1, UuidBinary.toBytes(player));
            try (var resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                final String key = resultSet.getString("key");
                assertEquals("PRIMARY", key);
            }
        }
    }
}
