package io.github.viniciussambinello.stags.infrastructure.storage.mysql;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.github.viniciussambinello.stags.infrastructure.config.MySqlConfig;

@Tag("mysql-integration")
final class SchemaMigratorTest {

    private String tablePrefix;
    private HikariConnectionProvider connectionProvider;

    @AfterEach
    void tearDown() throws SQLException {
        if (connectionProvider != null) {
            MySqlTestSupport.dropTables(connectionProvider, tablePrefix);
            connectionProvider.close();
        }
    }

    @Test
    void freshDatabaseCreatesTablesAndRecordsVersion() throws SQLException {
        tablePrefix = MySqlTestSupport.freshTablePrefix();
        final MySqlConfig config = MySqlTestSupport.config(tablePrefix);
        connectionProvider = new HikariConnectionProvider(config);

        new SchemaMigrator(connectionProvider, tablePrefix).migrate();

        try (Connection connection = connectionProvider.getConnection();
                Statement statement = connection.createStatement();
                var resultSet = statement.executeQuery("SELECT version FROM " + tablePrefix + "schema_version")) {
            assertEquals(true, resultSet.next());
            assertEquals(SchemaMigrator.CURRENT_VERSION, resultSet.getInt("version"));
        }
    }

    @Test
    void secondStartupAgainstExistingSchemaPreservesData() throws SQLException {
        tablePrefix = MySqlTestSupport.freshTablePrefix();
        final MySqlConfig config = MySqlTestSupport.config(tablePrefix);
        connectionProvider = new HikariConnectionProvider(config);
        new SchemaMigrator(connectionProvider, tablePrefix).migrate();

        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO " + tablePrefix + "cosmetic (kind, id, prefix, permission, weight) VALUES (?, ?, ?, ?, ?)")) {
            statement.setString(1, "TAG");
            statement.setString(2, "vip");
            statement.setString(3, "<gold>[VIP]</gold>");
            statement.setString(4, "stags.tag.vip");
            statement.setInt(5, 100);
            statement.executeUpdate();
        }

        assertDoesNotThrow(() -> new SchemaMigrator(connectionProvider, tablePrefix).migrate());

        try (Connection connection = connectionProvider.getConnection();
                Statement statement = connection.createStatement();
                var resultSet = statement.executeQuery("SELECT COUNT(*) AS total FROM " + tablePrefix + "cosmetic")) {
            resultSet.next();
            assertEquals(1, resultSet.getInt("total"));
        }
    }

    @Test
    void futureSchemaVersionAbortsStartup() throws SQLException {
        tablePrefix = MySqlTestSupport.freshTablePrefix();
        final MySqlConfig config = MySqlTestSupport.config(tablePrefix);
        connectionProvider = new HikariConnectionProvider(config);
        new SchemaMigrator(connectionProvider, tablePrefix).migrate();

        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO " + tablePrefix + "schema_version (version) VALUES (?)")) {
            statement.setInt(1, SchemaMigrator.CURRENT_VERSION + 1);
            statement.executeUpdate();
        }

        assertThrows(IllegalStateException.class, () -> new SchemaMigrator(connectionProvider, tablePrefix).migrate());
    }
}
