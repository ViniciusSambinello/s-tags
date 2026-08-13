package io.github.viniciussambinello.stags.infrastructure.selector;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import io.github.viniciussambinello.stags.application.service.SelectorCooldownService;
import io.github.viniciussambinello.stags.application.service.SelectorService;
import io.github.viniciussambinello.stags.application.usecase.ClearCosmetic;
import io.github.viniciussambinello.stags.application.usecase.SelectCosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.infrastructure.concurrent.MainThreadDispatcher;
import io.github.viniciussambinello.stags.infrastructure.config.ConfigService;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public final class ChatSelector {

    private static final Duration CALLBACK_LIFETIME = Duration.ofMinutes(2);

    private final ConfigService configService;
    private final SelectorService selectorService;
    private final SelectCosmetic selectCosmetic;
    private final ClearCosmetic clearCosmetic;
    private final SelectorCooldownService cooldownService;
    private final MainThreadDispatcher dispatcher;
    private final Clock clock;
    private final Map<UUID, Instant> openedAt;

    public ChatSelector(
            final ConfigService configService,
            final SelectorService selectorService,
            final SelectCosmetic selectCosmetic,
            final ClearCosmetic clearCosmetic,
            final SelectorCooldownService cooldownService,
            final MainThreadDispatcher dispatcher,
            final Clock clock) {
        this.configService = configService;
        this.selectorService = selectorService;
        this.selectCosmetic = selectCosmetic;
        this.clearCosmetic = clearCosmetic;
        this.cooldownService = cooldownService;
        this.dispatcher = dispatcher;
        this.clock = clock;
        this.openedAt = new ConcurrentHashMap<>();
    }

    public void open(final Player player, final CosmeticKind kind) {
        final var selectorConfig = configService.config().selector();
        if (!cooldownService.tryOpen(player.getUniqueId(), Duration.ofSeconds(selectorConfig.cooldowns().openSeconds()))) {
            sendMessage(player, "selector.cooldown");
            return;
        }

        final List<SelectorService.Entry> entries =
                selectorService.entries(player.getUniqueId(), kind, selectorConfig.hideLockedEntries());
        if (entries.isEmpty()) {
            sendMessage(player, "selector.empty", Placeholder.component("kind", kindComponent(kind)));
            return;
        }

        openedAt.put(player.getUniqueId(), clock.instant());
        for (final SelectorService.Entry entry : entries) {
            player.sendMessage(buildLine(player, kind, entry, selectorConfig.chat().showLockedPermission()));
        }
        if (selectorConfig.alwaysShowClearAction() || selectorService.hasActiveSelection(player.getUniqueId(), kind)) {
            player.sendMessage(buildClearLine(player, kind));
        }
    }

    private Component buildLine(final Player player, final CosmeticKind kind, final SelectorService.Entry entry, final boolean showLockedPermission) {
        final Component rendered = entry.cosmetic().prefix().rendered();
        if (!entry.owned()) {
            Component line = rendered;
            if (showLockedPermission) {
                line = line.hoverEvent(HoverEvent.showText(Component.text(entry.cosmetic().permission().value())));
            }
            return line;
        }
        final UUID playerId = player.getUniqueId();
        final ClickCallback<Audience> callback = audience -> {
            if (audience instanceof Player clicker) {
                onEntryClicked(clicker, kind, entry);
            }
        };
        final ClickCallback.Options options = ClickCallback.Options.builder()
                .uses(ClickCallback.UNLIMITED_USES)
                .lifetime(CALLBACK_LIFETIME)
                .build();
        return rendered
                .clickEvent(ClickEvent.callback(callback, options))
                .hoverEvent(HoverEvent.showText(rendered));
    }

    private Component buildClearLine(final Player player, final CosmeticKind kind) {
        final ClickCallback<Audience> callback = audience -> {
            if (audience instanceof Player clicker) {
                onClearClicked(clicker, kind);
            }
        };
        final ClickCallback.Options options = ClickCallback.Options.builder()
                .uses(ClickCallback.UNLIMITED_USES)
                .lifetime(CALLBACK_LIFETIME)
                .build();
        return Component.text("[Clear]").clickEvent(ClickEvent.callback(callback, options));
    }

    void onEntryClicked(final Player player, final CosmeticKind kind, final SelectorService.Entry entry) {
        if (isExpired(player.getUniqueId())) {
            sendMessage(player, "selector.expired");
            return;
        }
        if (!entry.owned()) {
            sendMessage(player, "selector.locked");
            return;
        }
        final var cooldowns = configService.config().selector().cooldowns();
        if (!cooldownService.trySelect(player.getUniqueId(), Duration.ofSeconds(cooldowns.selectSeconds()))) {
            sendMessage(player, "selector.cooldown");
            return;
        }
        selectCosmetic.execute(player.getUniqueId(), kind, entry.cosmetic().id()).whenComplete((result, throwable) ->
                dispatcher.run(() -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (throwable != null || result instanceof SelectCosmetic.Result.StorageFailure) {
                        sendMessage(player, "storage.failure");
                    } else if (result instanceof SelectCosmetic.Result.UnknownCosmetic) {
                        sendMessage(player, "selector.unknown-entry");
                    } else if (result instanceof SelectCosmetic.Result.NotOwned) {
                        sendMessage(player, "selection.not-owned", Placeholder.component("kind", kindComponent(kind)));
                    } else if (result instanceof SelectCosmetic.Result.Applied applied) {
                        sendMessage(player, "selection.selected",
                                Placeholder.component("kind", kindComponent(kind)),
                                Placeholder.component("name", applied.cosmetic().prefix().rendered()));
                    }
                }));
    }

    void onClearClicked(final Player player, final CosmeticKind kind) {
        if (isExpired(player.getUniqueId())) {
            sendMessage(player, "selector.expired");
            return;
        }
        final var cooldowns = configService.config().selector().cooldowns();
        if (!cooldownService.trySelect(player.getUniqueId(), Duration.ofSeconds(cooldowns.selectSeconds()))) {
            sendMessage(player, "selector.cooldown");
            return;
        }
        clearCosmetic.execute(player.getUniqueId(), kind).whenComplete((result, throwable) -> dispatcher.run(() -> {
            if (!player.isOnline()) {
                return;
            }
            if (throwable != null || result instanceof ClearCosmetic.Result.StorageFailure) {
                sendMessage(player, "storage.failure");
            } else {
                sendMessage(player, "selection.cleared", Placeholder.component("kind", kindComponent(kind)));
            }
        }));
    }

    private boolean isExpired(final UUID playerId) {
        final Instant opened = openedAt.get(playerId);
        return opened == null || Duration.between(opened, clock.instant()).compareTo(CALLBACK_LIFETIME) > 0;
    }

    private Component kindComponent(final CosmeticKind kind) {
        return Component.text(kind.name().toLowerCase(Locale.ROOT));
    }

    private void sendMessage(final Player player, final String key, final TagResolver... placeholders) {
        configService.messages().render(key, placeholders).ifPresent(player::sendMessage);
    }
}
