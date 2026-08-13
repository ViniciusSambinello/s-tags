package io.github.viniciussambinello.stags.infrastructure.storage.yaml;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import io.github.viniciussambinello.stags.application.port.SelectionRepository;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticId;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.domain.player.PlayerCosmetics;
import io.github.viniciussambinello.stags.domain.player.Selection;
import io.github.viniciussambinello.stags.infrastructure.concurrent.MainThreadGuard;
import io.github.viniciussambinello.stags.infrastructure.concurrent.StorageExecutor;

public final class YamlSelectionRepository implements SelectionRepository, AutoCloseable {

    private static final String STATE_UNSET = "UNSET";
    private static final String STATE_ACTIVE = "ACTIVE";
    private static final String STATE_CLEARED = "CLEARED";

    private final Path filePath;
    private final StorageExecutor executor;
    private final MainThreadGuard guard;
    private final DebouncedWriter writer;
    private final Map<UUID, PlayerCosmetics> state;
    private volatile boolean loaded;

    public YamlSelectionRepository(
            final Path filePath,
            final StorageExecutor executor,
            final MainThreadGuard guard,
            final Duration writeInterval) {
        this.filePath = filePath;
        this.executor = executor;
        this.guard = guard;
        this.state = new HashMap<>();
        this.writer = new DebouncedWriter(writeInterval, executor, () -> AtomicFileWriter.write(filePath, serialize(state)));
    }

    @Override
    public CompletableFuture<PlayerCosmetics> load(final UUID playerId) {
        return executor.submit(() -> {
            guard.assertOffMainThread("SelectionRepository.load");
            if (!loaded) {
                populateFromDisk();
                loaded = true;
            }
            return state.getOrDefault(playerId, PlayerCosmetics.unset(playerId));
        });
    }

    @Override
    public CompletableFuture<Void> save(final UUID playerId, final CosmeticKind kind, final Selection selection) {
        return executor.run(() -> {
            guard.assertOffMainThread("SelectionRepository.save");
            final PlayerCosmetics current = state.getOrDefault(playerId, PlayerCosmetics.unset(playerId));
            state.put(playerId, current.withSelection(kind, selection));
            writer.markDirty();
        });
    }

    @Override
    public void close() {
        writer.close();
    }

    private void populateFromDisk() {
        state.clear();
        final YamlConfiguration document = YamlFiles.loadOrEmpty(filePath);
        for (final String rawUuid : document.getKeys(false)) {
            final ConfigurationSection playerSection = document.getConfigurationSection(rawUuid);
            if (playerSection == null) {
                continue;
            }
            final UUID playerId = UUID.fromString(rawUuid);
            final Selection tag = readSelection(playerSection.getConfigurationSection(CosmeticKind.TAG.name().toLowerCase(Locale.ROOT)));
            final Selection title = readSelection(playerSection.getConfigurationSection(CosmeticKind.TITLE.name().toLowerCase(Locale.ROOT)));
            state.put(playerId, new PlayerCosmetics(playerId, tag, title));
        }
    }

    private Selection readSelection(final ConfigurationSection section) {
        if (section == null) {
            return Selection.UNSET;
        }
        final String stateValue = section.getString("state", STATE_UNSET);
        return switch (stateValue) {
            case STATE_ACTIVE -> new Selection.Active(new CosmeticId(section.getString("id", "")));
            case STATE_CLEARED -> Selection.CLEARED;
            default -> Selection.UNSET;
        };
    }

    private static String serialize(final Map<UUID, PlayerCosmetics> state) {
        final YamlConfiguration document = new YamlConfiguration();
        for (final PlayerCosmetics playerCosmetics : state.values()) {
            final String base = playerCosmetics.playerId().toString();
            writeSelection(document, base + "." + CosmeticKind.TAG.name().toLowerCase(Locale.ROOT), playerCosmetics.tagSelection());
            writeSelection(document, base + "." + CosmeticKind.TITLE.name().toLowerCase(Locale.ROOT), playerCosmetics.titleSelection());
        }
        return document.saveToString();
    }

    private static void writeSelection(final YamlConfiguration document, final String path, final Selection selection) {
        switch (selection) {
            case Selection.Active active -> {
                document.set(path + ".state", STATE_ACTIVE);
                document.set(path + ".id", active.cosmeticId().value());
            }
            case Selection.Cleared cleared -> document.set(path + ".state", STATE_CLEARED);
            case Selection.Unset unset -> document.set(path + ".state", STATE_UNSET);
        }
    }
}
