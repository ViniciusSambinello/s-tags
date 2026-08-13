package io.github.viniciussambinello.stags.infrastructure.config;

import java.util.List;

public final class ConfigDefaults {

    public static final StorageBackend STORAGE_BACKEND = StorageBackend.YAML;

    public static final String MYSQL_HOST = "localhost";
    public static final int MYSQL_PORT = 3306;
    public static final String MYSQL_DATABASE = "minecraft";
    public static final String MYSQL_USERNAME = "s_tags";
    public static final String MYSQL_PASSWORD = "change-me";
    public static final String MYSQL_TABLE_PREFIX = "stags_";
    public static final int MYSQL_POOL_SIZE = 8;
    public static final FailurePolicy MYSQL_FAILURE_POLICY = FailurePolicy.ABORT;

    public static final int YAML_WRITE_INTERVAL_SECONDS = 5;

    public static final SelectorMode SELECTOR_MODE = SelectorMode.MENU;
    public static final boolean SELECTOR_HIDE_LOCKED = false;
    public static final boolean SELECTOR_ALWAYS_SHOW_CLEAR = false;

    public static final int MENU_SIZE = 54;
    public static final List<Integer> MENU_CONTENT_SLOTS = List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34);
    public static final String MENU_ENTRY_MATERIAL = "PLAYER_HEAD";
    public static final String MENU_LOCKED_MATERIAL = "GRAY_STAINED_GLASS_PANE";
    public static final String MENU_FILLER_MATERIAL = "BLACK_STAINED_GLASS_PANE";
    public static final int MENU_PREVIOUS_PAGE_SLOT = 45;
    public static final int MENU_NEXT_PAGE_SLOT = 53;
    public static final int MENU_CLEAR_SELECTION_SLOT = 49;

    public static final boolean CHAT_SELECTOR_SHOW_LOCKED_PERMISSION = true;

    public static final int COOLDOWN_OPEN_SECONDS = 1;
    public static final int COOLDOWN_SELECT_SECONDS = 2;

    public static final boolean CHAT_RENDER_ENABLED = true;
    public static final String CHAT_RENDER_FORMAT = "<tag_prefix> <white><player></white><gray>:</gray> <message>";
    public static final String CHAT_FORMATTING_PERMISSION = "stags.chat.format";

    public static final boolean NAMETAG_ENABLED = true;

    public static final boolean TAB_LIST_ENABLED = true;
    public static final TabListOrdering TAB_LIST_ORDERING = TabListOrdering.WEIGHT;

    public static final boolean TITLE_HOLOGRAM_ENABLED = true;
    public static final double TITLE_HOLOGRAM_VERTICAL_OFFSET = 0.35;
    public static final boolean TITLE_HOLOGRAM_SELF_VISIBLE = false;

    public static final boolean RECONCILIATION_ENABLED = false;
    public static final int RECONCILIATION_INTERVAL_SECONDS = 300;

    public static final int AUTHORING_FLOW_TIMEOUT_SECONDS = 120;
    public static final String AUTHORING_DEFAULT_PERMISSION_PATTERN = "stags.{kind}.{id}";

    public static final boolean COMMAND_ALLOW_FORCE_UNOWNED = false;

    public static final List<String> KNOWN_PATHS = List.of(
            "storage.backend",
            "storage.mysql.host",
            "storage.mysql.port",
            "storage.mysql.database",
            "storage.mysql.username",
            "storage.mysql.password",
            "storage.mysql.table-prefix",
            "storage.mysql.pool-size",
            "storage.mysql.failure-policy",
            "storage.yaml.write-interval-seconds",
            "selector.mode",
            "selector.hide-locked-entries",
            "selector.always-show-clear-action",
            "selector.menu.size",
            "selector.menu.content-slots",
            "selector.menu.entry-material",
            "selector.menu.locked-material",
            "selector.menu.filler-material",
            "selector.menu.previous-page-slot",
            "selector.menu.next-page-slot",
            "selector.menu.clear-selection-slot",
            "selector.chat.show-locked-permission",
            "selector.cooldowns.open-seconds",
            "selector.cooldowns.select-seconds",
            "render.chat.enabled",
            "render.chat.format",
            "render.chat.formatting-permission",
            "render.nametag.enabled",
            "render.tab-list.enabled",
            "render.tab-list.ordering",
            "render.title-hologram.enabled",
            "render.title-hologram.vertical-offset",
            "render.title-hologram.self-visible",
            "render.reconciliation.enabled",
            "render.reconciliation.interval-seconds",
            "authoring.flow-timeout-seconds",
            "authoring.default-permission-pattern",
            "command.allow-force-unowned");

    public static StagsConfig buildDefault() {
        return new StagsConfig(
                new StorageConfig(
                        STORAGE_BACKEND,
                        new MySqlConfig(MYSQL_HOST, MYSQL_PORT, MYSQL_DATABASE, MYSQL_USERNAME, MYSQL_PASSWORD,
                                MYSQL_TABLE_PREFIX, MYSQL_POOL_SIZE, MYSQL_FAILURE_POLICY),
                        new YamlStorageConfig(YAML_WRITE_INTERVAL_SECONDS)),
                new SelectorConfig(
                        SELECTOR_MODE,
                        SELECTOR_HIDE_LOCKED,
                        SELECTOR_ALWAYS_SHOW_CLEAR,
                        new MenuLayoutConfig(MENU_SIZE, MENU_CONTENT_SLOTS, MENU_ENTRY_MATERIAL, MENU_LOCKED_MATERIAL,
                                MENU_FILLER_MATERIAL, MENU_PREVIOUS_PAGE_SLOT, MENU_NEXT_PAGE_SLOT,
                                MENU_CLEAR_SELECTION_SLOT),
                        new ChatSelectorConfig(CHAT_SELECTOR_SHOW_LOCKED_PERMISSION),
                        new CooldownConfig(COOLDOWN_OPEN_SECONDS, COOLDOWN_SELECT_SECONDS)),
                new RenderConfig(
                        new ChatRenderConfig(CHAT_RENDER_ENABLED, CHAT_RENDER_FORMAT, CHAT_FORMATTING_PERMISSION),
                        NAMETAG_ENABLED,
                        new TabListRenderConfig(TAB_LIST_ENABLED, TAB_LIST_ORDERING),
                        new TitleHologramConfig(TITLE_HOLOGRAM_ENABLED, TITLE_HOLOGRAM_VERTICAL_OFFSET, TITLE_HOLOGRAM_SELF_VISIBLE),
                        new ReconciliationConfig(RECONCILIATION_ENABLED, RECONCILIATION_INTERVAL_SECONDS)),
                new AuthoringConfig(AUTHORING_FLOW_TIMEOUT_SECONDS, AUTHORING_DEFAULT_PERMISSION_PATTERN),
                new CommandConfig(COMMAND_ALLOW_FORCE_UNOWNED));
    }

    private ConfigDefaults() {
    }
}
