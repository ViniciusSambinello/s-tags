package io.github.viniciussambinello.stags.infrastructure.storage.mysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class SchemaMigrator {

    static final int CURRENT_VERSION = 1;

    private final HikariConnectionProvider connectionProvider;
    private final String tablePrefix;

    public SchemaMigrator(final HikariConnectionProvider connectionProvider, final String rawTablePrefix) {
        this.connectionProvider = connectionProvider;
        this.tablePrefix = TablePrefix.validate(rawTablePrefix);
    }

    public void migrate() throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            createSchemaVersionTable(connection);
            final int storedVersion = readStoredVersion(connection);
            if (storedVersion > CURRENT_VERSION) {
                throw new IllegalStateException(
                        "Database schema version " + storedVersion
                                + " is newer than this plugin's supported version " + CURRENT_VERSION);
            }
            for (int version = storedVersion + 1; version <= CURRENT_VERSION; version++) {
                applyMigration(connection, version);
                recordVersion(connection, version);
            }
        }
    }

    private void createSchemaVersionTable(final Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS " + table("schema_version") + " ("
                    + "version INT NOT NULL PRIMARY KEY, "
                    + "applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
                    + ") ENGINE=InnoDB");
        }
    }

    private int readStoredVersion(final Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                var resultSet = statement.executeQuery("SELECT MAX(version) AS version FROM " + table("schema_version"))) {
            if (resultSet.next()) {
                final int version = resultSet.getInt("version");
                return resultSet.wasNull() ? 0 : version;
            }
            return 0;
        }
    }

    private void recordVersion(final Connection connection, final int version) throws SQLException {
        try (var statement = connection.prepareStatement("INSERT INTO " + table("schema_version") + " (version) VALUES (?)")) {
            statement.setInt(1, version);
            statement.executeUpdate();
        }
    }

    private void applyMigration(final Connection connection, final int version) throws SQLException {
        switch (version) {
            case 1 -> applyVersion1(connection);
            default -> throw new IllegalStateException("No migration defined for schema version " + version);
        }
    }

    private void applyVersion1(final Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS " + table("cosmetic") + " ("
                    + "kind VARCHAR(8) NOT NULL, "
                    + "id VARCHAR(32) NOT NULL, "
                    + "prefix VARCHAR(255) NOT NULL, "
                    + "permission VARCHAR(128) NOT NULL, "
                    + "weight INT NOT NULL, "
                    + "PRIMARY KEY (kind, id)"
                    + ") ENGINE=InnoDB");
            statement.execute("CREATE TABLE IF NOT EXISTS " + table("player_selection") + " ("
                    + "player_uuid BINARY(16) NOT NULL, "
                    + "kind VARCHAR(8) NOT NULL, "
                    + "cosmetic_id VARCHAR(32) NULL, "
                    + "cleared BOOLEAN NOT NULL DEFAULT FALSE, "
                    + "PRIMARY KEY (player_uuid, kind)"
                    + ") ENGINE=InnoDB");
        }
    }

    private String table(final String suffix) {
        return tablePrefix + suffix;
    }
}
