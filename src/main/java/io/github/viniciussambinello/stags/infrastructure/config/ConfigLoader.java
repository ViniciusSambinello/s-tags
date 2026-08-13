package io.github.viniciussambinello.stags.infrastructure.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ConfigLoader {

    public record LoadResult(StagsConfig config, List<String> warnings) {
    }

    public LoadResult load(final YamlConfiguration source) {
        final ConfigValueReader reader = new ConfigValueReader(source);
        final List<String> warnings = new ArrayList<>();

        final StorageConfig storage = readStorage(reader);
        final SelectorConfig selector = readSelector(reader);
        final RenderConfig render = readRender(reader);
        final AuthoringConfig authoring = readAuthoring(reader);
        final CommandConfig command = readCommand(reader);

        warnings.addAll(reader.warnings());
        warnings.addAll(findUnrecognizedKeys(source));

        final StagsConfig config = new StagsConfig(storage, selector, render, authoring, command);
        return new LoadResult(config, List.copyOf(warnings));
    }

    private StorageConfig readStorage(final ConfigValueReader reader) {
        final StorageBackend backend = reader.getEnum("storage.backend", StorageBackend.class, ConfigDefaults.STORAGE_BACKEND);
        final MySqlConfig mysql = new MySqlConfig(
                reader.getString("storage.mysql.host", ConfigDefaults.MYSQL_HOST),
                reader.getInt("storage.mysql.port", ConfigDefaults.MYSQL_PORT, 1, 65535),
                reader.getString("storage.mysql.database", ConfigDefaults.MYSQL_DATABASE),
                reader.getString("storage.mysql.username", ConfigDefaults.MYSQL_USERNAME),
                reader.getString("storage.mysql.password", ConfigDefaults.MYSQL_PASSWORD),
                reader.getString("storage.mysql.table-prefix", ConfigDefaults.MYSQL_TABLE_PREFIX),
                reader.getInt("storage.mysql.pool-size", ConfigDefaults.MYSQL_POOL_SIZE, 1, 100),
                reader.getEnum("storage.mysql.failure-policy", FailurePolicy.class, ConfigDefaults.MYSQL_FAILURE_POLICY));
        final YamlStorageConfig yaml = new YamlStorageConfig(
                reader.getInt("storage.yaml.write-interval-seconds", ConfigDefaults.YAML_WRITE_INTERVAL_SECONDS, 1, 3600));
        return new StorageConfig(backend, mysql, yaml);
    }

    private SelectorConfig readSelector(final ConfigValueReader reader) {
        final int menuSize = readMenuSize(reader);
        final MenuLayoutConfig menu = new MenuLayoutConfig(
                menuSize,
                reader.getIntList("selector.menu.content-slots", ConfigDefaults.MENU_CONTENT_SLOTS),
                reader.getString("selector.menu.entry-material", ConfigDefaults.MENU_ENTRY_MATERIAL),
                reader.getString("selector.menu.locked-material", ConfigDefaults.MENU_LOCKED_MATERIAL),
                reader.getString("selector.menu.filler-material", ConfigDefaults.MENU_FILLER_MATERIAL),
                reader.getInt("selector.menu.previous-page-slot", ConfigDefaults.MENU_PREVIOUS_PAGE_SLOT, 0, 53),
                reader.getInt("selector.menu.next-page-slot", ConfigDefaults.MENU_NEXT_PAGE_SLOT, 0, 53),
                reader.getInt("selector.menu.clear-selection-slot", ConfigDefaults.MENU_CLEAR_SELECTION_SLOT, 0, 53));
        final ChatSelectorConfig chat = new ChatSelectorConfig(
                reader.getBoolean("selector.chat.show-locked-permission", ConfigDefaults.CHAT_SELECTOR_SHOW_LOCKED_PERMISSION));
        final CooldownConfig cooldowns = new CooldownConfig(
                reader.getInt("selector.cooldowns.open-seconds", ConfigDefaults.COOLDOWN_OPEN_SECONDS, 0, 3600),
                reader.getInt("selector.cooldowns.select-seconds", ConfigDefaults.COOLDOWN_SELECT_SECONDS, 0, 3600));
        return new SelectorConfig(
                reader.getEnum("selector.mode", SelectorMode.class, ConfigDefaults.SELECTOR_MODE),
                reader.getBoolean("selector.hide-locked-entries", ConfigDefaults.SELECTOR_HIDE_LOCKED),
                reader.getBoolean("selector.always-show-clear-action", ConfigDefaults.SELECTOR_ALWAYS_SHOW_CLEAR),
                menu,
                chat,
                cooldowns);
    }

    private int readMenuSize(final ConfigValueReader reader) {
        final int size = reader.getInt("selector.menu.size", ConfigDefaults.MENU_SIZE, 9, 54);
        if (size % 9 != 0) {
            reader.warn("Value for 'selector.menu.size' (" + size + ") is not a multiple of 9. Using default "
                    + ConfigDefaults.MENU_SIZE + ".");
            return ConfigDefaults.MENU_SIZE;
        }
        return size;
    }

    private RenderConfig readRender(final ConfigValueReader reader) {
        final ChatRenderConfig chat = new ChatRenderConfig(
                reader.getBoolean("render.chat.enabled", ConfigDefaults.CHAT_RENDER_ENABLED),
                reader.getString("render.chat.format", ConfigDefaults.CHAT_RENDER_FORMAT),
                reader.getString("render.chat.formatting-permission", ConfigDefaults.CHAT_FORMATTING_PERMISSION));
        final TabListRenderConfig tabList = new TabListRenderConfig(
                reader.getBoolean("render.tab-list.enabled", ConfigDefaults.TAB_LIST_ENABLED),
                reader.getEnum("render.tab-list.ordering", TabListOrdering.class, ConfigDefaults.TAB_LIST_ORDERING));
        final TitleHologramConfig titleHologram = new TitleHologramConfig(
                reader.getBoolean("render.title-hologram.enabled", ConfigDefaults.TITLE_HOLOGRAM_ENABLED),
                reader.getDouble("render.title-hologram.vertical-offset", ConfigDefaults.TITLE_HOLOGRAM_VERTICAL_OFFSET, -10.0, 10.0),
                reader.getBoolean("render.title-hologram.self-visible", ConfigDefaults.TITLE_HOLOGRAM_SELF_VISIBLE));
        final ReconciliationConfig reconciliation = new ReconciliationConfig(
                reader.getBoolean("render.reconciliation.enabled", ConfigDefaults.RECONCILIATION_ENABLED),
                reader.getInt("render.reconciliation.interval-seconds", ConfigDefaults.RECONCILIATION_INTERVAL_SECONDS, 1, 86400));
        return new RenderConfig(
                chat,
                reader.getBoolean("render.nametag.enabled", ConfigDefaults.NAMETAG_ENABLED),
                tabList,
                titleHologram,
                reconciliation);
    }

    private AuthoringConfig readAuthoring(final ConfigValueReader reader) {
        return new AuthoringConfig(
                reader.getInt("authoring.flow-timeout-seconds", ConfigDefaults.AUTHORING_FLOW_TIMEOUT_SECONDS, 1, 3600),
                reader.getString("authoring.default-permission-pattern", ConfigDefaults.AUTHORING_DEFAULT_PERMISSION_PATTERN));
    }

    private CommandConfig readCommand(final ConfigValueReader reader) {
        return new CommandConfig(
                reader.getBoolean("command.allow-force-unowned", ConfigDefaults.COMMAND_ALLOW_FORCE_UNOWNED));
    }

    private List<String> findUnrecognizedKeys(final ConfigurationSection root) {
        final Set<String> knownPaths = new LinkedHashSet<>(ConfigDefaults.KNOWN_PATHS);
        final List<String> unrecognized = new ArrayList<>();
        for (final String leafPath : collectLeafPaths(root)) {
            if (!knownPaths.contains(leafPath)) {
                unrecognized.add("Unrecognized configuration key '" + leafPath + "' was ignored.");
            }
        }
        return unrecognized;
    }

    private List<String> collectLeafPaths(final ConfigurationSection section) {
        final List<String> leaves = new ArrayList<>();
        for (final String key : section.getKeys(false)) {
            final Object value = section.get(key);
            final String path = section.getCurrentPath().isEmpty() ? key : section.getCurrentPath() + "." + key;
            if (value instanceof ConfigurationSection nested) {
                leaves.addAll(collectLeafPaths(nested));
            } else {
                leaves.add(path);
            }
        }
        return leaves;
    }
}
