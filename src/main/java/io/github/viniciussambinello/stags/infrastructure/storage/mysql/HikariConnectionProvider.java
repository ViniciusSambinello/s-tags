package io.github.viniciussambinello.stags.infrastructure.storage.mysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.github.viniciussambinello.stags.infrastructure.config.MySqlConfig;

public final class HikariConnectionProvider implements AutoCloseable {

    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(10);

    private final HikariDataSource dataSource;

    public HikariConnectionProvider(final MySqlConfig config) {
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + config.host() + ":" + config.port() + "/" + config.database()
                + "?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true");
        hikariConfig.setUsername(config.username());
        hikariConfig.setPassword(config.password());
        hikariConfig.setMaximumPoolSize(config.poolSize());
        hikariConfig.setPoolName("s-tags-hikari");
        hikariConfig.setInitializationFailTimeout(-1);
        hikariConfig.setConnectionTimeout(CONNECTION_TIMEOUT.toMillis());
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        this.dataSource = new HikariDataSource(hikariConfig);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void testConnectivity() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(5)) {
                throw new SQLException("Connection validation failed");
            }
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
