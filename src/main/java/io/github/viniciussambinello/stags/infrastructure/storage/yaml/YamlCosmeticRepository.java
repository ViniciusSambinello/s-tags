package io.github.viniciussambinello.stags.infrastructure.storage.yaml;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

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

public final class YamlCosmeticRepository implements CosmeticRepository, AutoCloseable {

    private final Path filePath;
    private final StorageExecutor executor;
    private final MainThreadGuard guard;
    private final DebouncedWriter writer;
    private final Map<CosmeticKind, Map<CosmeticId, Cosmetic>> state;
    private volatile boolean loaded;

    public YamlCosmeticRepository(
            final Path filePath,
            final StorageExecutor executor,
            final MainThreadGuard guard,
            final Duration writeInterval) {
        this.filePath = filePath;
        this.executor = executor;
        this.guard = guard;
        this.state = new EnumMap<>(CosmeticKind.class);
        for (final CosmeticKind kind : CosmeticKind.values()) {
            state.put(kind, new HashMap<>());
        }
        this.writer = new DebouncedWriter(writeInterval, executor, () -> AtomicFileWriter.write(filePath, serialize(state)));
    }

    @Override
    public CompletableFuture<Catalogue> loadAll() {
        return executor.submit(() -> {
            guard.assertOffMainThread("CosmeticRepository.loadAll");
            if (!loaded) {
                populateFromDisk();
                loaded = true;
            }
            final List<Cosmetic> all = new ArrayList<>();
            state.values().forEach(byId -> all.addAll(byId.values()));
            return Catalogue.of(all);
        });
    }

    @Override
    public CompletableFuture<InsertOutcome> insert(final Cosmetic cosmetic) {
        return executor.submit(() -> {
            guard.assertOffMainThread("CosmeticRepository.insert");
            final Map<CosmeticId, Cosmetic> byId = state.get(cosmetic.kind());
            if (byId.containsKey(cosmetic.id())) {
                return InsertOutcome.DUPLICATE;
            }
            byId.put(cosmetic.id(), cosmetic);
            writer.markDirty();
            return InsertOutcome.CREATED;
        });
    }

    @Override
    public CompletableFuture<Void> update(final Cosmetic cosmetic) {
        return executor.run(() -> {
            guard.assertOffMainThread("CosmeticRepository.update");
            state.get(cosmetic.kind()).put(cosmetic.id(), cosmetic);
            writer.markDirty();
        });
    }

    @Override
    public CompletableFuture<Void> delete(final CosmeticKind kind, final CosmeticId id) {
        return executor.run(() -> {
            guard.assertOffMainThread("CosmeticRepository.delete");
            state.get(kind).remove(id);
            writer.markDirty();
        });
    }

    @Override
    public void close() {
        writer.close();
    }

    private void populateFromDisk() {
        state.values().forEach(Map::clear);
        final YamlConfiguration document = YamlFiles.loadOrEmpty(filePath);
        for (final CosmeticKind kind : CosmeticKind.values()) {
            final ConfigurationSection section = document.getConfigurationSection(kind.name().toLowerCase(Locale.ROOT));
            if (section == null) {
                continue;
            }
            for (final String rawId : section.getKeys(false)) {
                final ConfigurationSection entry = section.getConfigurationSection(rawId);
                if (entry == null) {
                    continue;
                }
                final Cosmetic cosmetic = new Cosmetic(
                        kind,
                        new CosmeticId(rawId),
                        Prefix.parseStored(entry.getString("prefix", "")),
                        new PermissionNode(entry.getString("permission", "")),
                        new Weight(entry.getInt("weight", 0)));
                state.get(kind).put(cosmetic.id(), cosmetic);
            }
        }
    }

    private static String serialize(final Map<CosmeticKind, Map<CosmeticId, Cosmetic>> state) {
        final YamlConfiguration document = new YamlConfiguration();
        for (final Map.Entry<CosmeticKind, Map<CosmeticId, Cosmetic>> kindEntry : state.entrySet()) {
            final String kindKey = kindEntry.getKey().name().toLowerCase(Locale.ROOT);
            for (final Cosmetic cosmetic : kindEntry.getValue().values()) {
                final String base = kindKey + "." + cosmetic.id().value();
                document.set(base + ".prefix", cosmetic.prefix().raw());
                document.set(base + ".permission", cosmetic.permission().value());
                document.set(base + ".weight", cosmetic.weight().value());
            }
        }
        return document.saveToString();
    }
}
