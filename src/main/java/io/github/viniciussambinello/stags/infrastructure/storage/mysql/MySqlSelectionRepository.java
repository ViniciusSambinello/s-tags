package io.github.viniciussambinello.stags.infrastructure.storage.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.github.viniciussambinello.stags.application.port.SelectionRepository;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.player.PlayerCosmetics;
import io.github.viniciussambinello.stags.domain.player.Selection;
import io.github.viniciussambinello.stags.infrastructure.concurrent.MainThreadGuard;
import io.github.viniciussambinello.stags.infrastructure.concurrent.StorageExecutor;

public final class MySqlSelectionRepository implements SelectionRepository {

    private final HikariConnectionProvider connectionProvider;
    private final StorageExecutor executor;
    private final MainThreadGuard guard;
    private final String tablePrefix;

    public MySqlSelectionRepository(
            final HikariConnectionProvider connectionProvider,
            final StorageExecutor executor,
            final MainThreadGuard guard,
            final String rawTablePrefix) {
        this.connectionProvider = connectionProvider;
        this.executor = executor;
        this.guard = guard;
        this.tablePrefix = TablePrefix.validate(rawTablePrefix);
    }

    @Override
    public CompletableFuture<PlayerCosmetics> load(final UUID playerId) {
        return executor.submit(() -> {
            guard.assertOffMainThread("SelectionRepository.load");
            Selection tag = Selection.UNSET;
            Selection title = Selection.UNSET;
            final String sql = "SELECT kind, cosmetic_id, cleared FROM " + table("player_selection") + " WHERE player_uuid = ?";
            try (Connection connection = connectionProvider.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setBytes(1, UuidBinary.toBytes(playerId));
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        final CosmeticKind kind = CosmeticKind.valueOf(resultSet.getString("kind"));
                        final Selection selection = readSelection(resultSet);
                        if (kind == CosmeticKind.TAG) {
                            tag = selection;
                        } else {
                            title = selection;
                        }
                    }
                }
            } catch (final SQLException exception) {
                throw new StorageException("Failed to load selections for " + playerId, exception);
            }
            return new PlayerCosmetics(playerId, tag, title);
        });
    }

    @Override
    public CompletableFuture<Void> save(final UUID playerId, final CosmeticKind kind, final Selection selection) {
        return executor.run(() -> {
            guard.assertOffMainThread("SelectionRepository.save");
            final String sql = "INSERT INTO " + table("player_selection") + " (player_uuid, kind, cosmetic_id, cleared) VALUES (?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE cosmetic_id = VALUES(cosmetic_id), cleared = VALUES(cleared)";
            try (Connection connection = connectionProvider.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setBytes(1, UuidBinary.toBytes(playerId));
                statement.setString(2, kind.name());
                if (selection instanceof Selection.Active active) {
                    statement.setString(3, active.cosmeticId().value());
                } else {
                    statement.setNull(3, Types.VARCHAR);
                }
                statement.setBoolean(4, selection instanceof Selection.Cleared);
                statement.executeUpdate();
            } catch (final SQLException exception) {
                throw new StorageException("Failed to save selection for " + playerId, exception);
            }
        });
    }

    private Selection readSelection(final ResultSet resultSet) throws SQLException {
        final String cosmeticId = resultSet.getString("cosmetic_id");
        if (cosmeticId != null) {
            return new Selection.Active(new CosmeticId(cosmeticId));
        }
        if (resultSet.getBoolean("cleared")) {
            return Selection.CLEARED;
        }
        return Selection.UNSET;
    }

    private String table(final String suffix) {
        return tablePrefix + suffix;
    }
}
