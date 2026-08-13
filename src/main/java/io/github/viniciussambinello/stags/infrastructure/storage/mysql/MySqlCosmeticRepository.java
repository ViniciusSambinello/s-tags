package io.github.viniciussambinello.stags.infrastructure.storage.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

public final class MySqlCosmeticRepository implements CosmeticRepository {

    private final HikariConnectionProvider connectionProvider;
    private final StorageExecutor executor;
    private final MainThreadGuard guard;
    private final String tablePrefix;

    public MySqlCosmeticRepository(
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
    public CompletableFuture<Catalogue> loadAll() {
        return executor.submit(() -> {
            guard.assertOffMainThread("CosmeticRepository.loadAll");
            final List<Cosmetic> cosmetics = new ArrayList<>();
            final String sql = "SELECT kind, id, prefix, permission, weight FROM " + table("cosmetic");
            try (Connection connection = connectionProvider.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    cosmetics.add(readCosmetic(resultSet));
                }
            } catch (final SQLException exception) {
                throw new StorageException("Failed to load cosmetics", exception);
            }
            return Catalogue.of(cosmetics);
        });
    }

    @Override
    public CompletableFuture<InsertOutcome> insert(final Cosmetic cosmetic) {
        return executor.submit(() -> {
            guard.assertOffMainThread("CosmeticRepository.insert");
            final String sql = "INSERT INTO " + table("cosmetic") + " (kind, id, prefix, permission, weight) VALUES (?, ?, ?, ?, ?)";
            try (Connection connection = connectionProvider.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, cosmetic.kind().name());
                statement.setString(2, cosmetic.id().value());
                statement.setString(3, cosmetic.prefix().raw());
                statement.setString(4, cosmetic.permission().value());
                statement.setInt(5, cosmetic.weight().value());
                statement.executeUpdate();
                return InsertOutcome.CREATED;
            } catch (final SQLIntegrityConstraintViolationException exception) {
                return InsertOutcome.DUPLICATE;
            } catch (final SQLException exception) {
                throw new StorageException("Failed to insert cosmetic " + cosmetic.id(), exception);
            }
        });
    }

    @Override
    public CompletableFuture<Void> update(final Cosmetic cosmetic) {
        return executor.run(() -> {
            guard.assertOffMainThread("CosmeticRepository.update");
            final String sql = "UPDATE " + table("cosmetic") + " SET prefix = ?, permission = ?, weight = ? WHERE kind = ? AND id = ?";
            try (Connection connection = connectionProvider.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, cosmetic.prefix().raw());
                statement.setString(2, cosmetic.permission().value());
                statement.setInt(3, cosmetic.weight().value());
                statement.setString(4, cosmetic.kind().name());
                statement.setString(5, cosmetic.id().value());
                statement.executeUpdate();
            } catch (final SQLException exception) {
                throw new StorageException("Failed to update cosmetic " + cosmetic.id(), exception);
            }
        });
    }

    @Override
    public CompletableFuture<Void> delete(final CosmeticKind kind, final CosmeticId id) {
        return executor.run(() -> {
            guard.assertOffMainThread("CosmeticRepository.delete");
            final String sql = "DELETE FROM " + table("cosmetic") + " WHERE kind = ? AND id = ?";
            try (Connection connection = connectionProvider.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, kind.name());
                statement.setString(2, id.value());
                statement.executeUpdate();
            } catch (final SQLException exception) {
                throw new StorageException("Failed to delete cosmetic " + id, exception);
            }
        });
    }

    private Cosmetic readCosmetic(final ResultSet resultSet) throws SQLException {
        return new Cosmetic(
                CosmeticKind.valueOf(resultSet.getString("kind")),
                new CosmeticId(resultSet.getString("id")),
                Prefix.parseStored(resultSet.getString("prefix")),
                new PermissionNode(resultSet.getString("permission")),
                new Weight(resultSet.getInt("weight")));
    }

    private String table(final String suffix) {
        return tablePrefix + suffix;
    }
}
