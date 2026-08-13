package io.github.viniciussambinello.stags.infrastructure.config;

import java.util.Objects;

public record SelectorConfig(
        SelectorMode mode,
        boolean hideLockedEntries,
        boolean alwaysShowClearAction,
        MenuLayoutConfig menu,
        ChatSelectorConfig chat,
        CooldownConfig cooldowns) {

    public SelectorConfig {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(menu, "menu");
        Objects.requireNonNull(chat, "chat");
        Objects.requireNonNull(cooldowns, "cooldowns");
    }
}
