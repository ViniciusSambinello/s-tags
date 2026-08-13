package io.github.viniciussambinello.stags.infrastructure.selector;

import org.bukkit.entity.Player;

import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.infrastructure.config.ConfigService;
import io.github.viniciussambinello.stags.infrastructure.config.SelectorMode;

public final class SelectorGateway {

    private final ConfigService configService;
    private final MenuSelector menuSelector;
    private final ChatSelector chatSelector;

    public SelectorGateway(final ConfigService configService, final MenuSelector menuSelector, final ChatSelector chatSelector) {
        this.configService = configService;
        this.menuSelector = menuSelector;
        this.chatSelector = chatSelector;
    }

    public void open(final Player player, final CosmeticKind kind) {
        if (configService.config().selector().mode() == SelectorMode.CHAT) {
            chatSelector.open(player, kind);
        } else {
            menuSelector.open(player, kind);
        }
    }
}
