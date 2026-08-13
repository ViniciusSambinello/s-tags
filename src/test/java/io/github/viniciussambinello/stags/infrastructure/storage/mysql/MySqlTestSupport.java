package io.github.viniciussambinello.stags.infrastructure.storage.mysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import io.github.viniciussambinello.stags.infrastructure.config.FailurePolicy;
import io.github.viniciussambinello.stags.infrastructure.config.MySqlConfig;

final class MySqlTestSupport {

    static MySqlConfig config(final String tablePrefix) {
        return new MySqlConfig(
                System.getProperty("stags.test.mysql.host", "127.0.0.1"),
                Integer.parseInt(System.getProperty("stags.test.mysql.port", "3307")),
                System.getProperty("stags.test.mysql.database", "stags_test"),
                System.getProperty("stags.test.mysql.username", "stags_test"),
                System.getProperty("stags.test.mysql.password", "stags_test_pw"),
                tablePrefix,
                4,
                FailurePolicy.ABORT);
    }

    static String freshTablePrefix() {
        return "t" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "_";
    }

    static void dropTables(final HikariConnectionProvider connectionProvider, final String tablePrefix) throws SQLException {
        try (Connection connection = connectionProvider.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS " + tablePrefix + "player_selection");
            statement.execute("DROP TABLE IF EXISTS " + tablePrefix + "cosmetic");
            statement.execute("DROP TABLE IF EXISTS " + tablePrefix + "schema_version");
        }
    }

    private MySqlTestSupport() {
    }
}
