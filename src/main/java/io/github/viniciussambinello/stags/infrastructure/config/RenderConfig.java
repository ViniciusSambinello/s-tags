package io.github.viniciussambinello.stags.infrastructure.config;

import java.util.Objects;

public record RenderConfig(
        ChatRenderConfig chat,
        boolean nametagEnabled,
        TabListRenderConfig tabList,
        TitleHologramConfig titleHologram,
        ReconciliationConfig reconciliation) {

    public RenderConfig {
        Objects.requireNonNull(chat, "chat");
        Objects.requireNonNull(tabList, "tabList");
        Objects.requireNonNull(titleHologram, "titleHologram");
        Objects.requireNonNull(reconciliation, "reconciliation");
    }
}
