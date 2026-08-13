package io.github.viniciussambinello.stags.infrastructure.selector;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.viniciussambinello.stags.application.service.SelectorCooldownService;
import io.github.viniciussambinello.stags.application.service.SelectorService;
import io.github.viniciussambinello.stags.application.usecase.ClearCosmetic;
import io.github.viniciussambinello.stags.application.usecase.SelectCosmetic;
import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;
import io.github.viniciussambinello.stags.infrastructure.concurrent.MainThreadDispatcher;
import io.github.viniciussambinello.stags.infrastructure.config.ConfigService;
import io.github.viniciussambinello.stags.infrastructure.config.MenuLayoutConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public final class MenuSelector implements Listener {

    private final ConfigService configService;
    private final SelectorService selectorService;
    private final SelectCosmetic selectCosmetic;
    private final ClearCosmetic clearCosmetic;
    private final SelectorCooldownService cooldownService;
    private final MainThreadDispatcher dispatcher;
    private final Server server;

    public MenuSelector(
            final ConfigService configService,
            final SelectorService selectorService,
            final SelectCosmetic selectCosmetic,
            final ClearCosmetic clearCosmetic,
            final SelectorCooldownService cooldownService,
            final MainThreadDispatcher dispatcher,
            final Server server) {
        this.configService = configService;
        this.selectorService = selectorService;
        this.selectCosmetic = selectCosmetic;
        this.clearCosmetic = clearCosmetic;
        this.cooldownService = cooldownService;
        this.dispatcher = dispatcher;
        this.server = server;
    }

    public void open(final Player player, final CosmeticKind kind) {
        openPage(player, kind, 0, true);
    }

    private void openPage(final Player player, final CosmeticKind kind, final int requestedPage, final boolean enforceCooldown) {
        final var selectorConfig = configService.config().selector();
        if (enforceCooldown
                && !cooldownService.tryOpen(player.getUniqueId(), Duration.ofSeconds(selectorConfig.cooldowns().openSeconds()))) {
            sendMessage(player, "selector.cooldown");
            return;
        }

        final List<SelectorService.Entry> entries =
                selectorService.entries(player.getUniqueId(), kind, selectorConfig.hideLockedEntries());
        if (entries.isEmpty()) {
            sendMessage(player, "selector.empty", Placeholder.component("kind", kindComponent(kind)));
            return;
        }

        final MenuLayoutConfig layout = selectorConfig.menu();
        final int perPage = Math.max(1, layout.contentSlots().size());
        final int totalPages = Math.max(1, (entries.size() + perPage - 1) / perPage);
        final int page = Math.max(0, Math.min(requestedPage, totalPages - 1));

        final MenuHolder holder = new MenuHolder(player.getUniqueId(), kind, page);
        final Component title = configService.messages().render("selector.menu-title")
                .orElse(Component.text("Select a cosmetic"));
        final Inventory inventory = server.createInventory(holder, layout.size(), title);
        holder.attach(inventory);

        fillBackground(inventory, layout);
        populateEntries(inventory, layout, entries, page, perPage, player);
        populateNavigation(inventory, layout, page, totalPages);
        populateClear(inventory, layout, kind, player, selectorConfig.alwaysShowClearAction());

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(final InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        final int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getInventory().getSize()) {
            return;
        }

        final MenuLayoutConfig layout = configService.config().selector().menu();
        if (rawSlot == layout.previousPageSlot()) {
            openPage(player, holder.kind(), holder.page() - 1, false);
            return;
        }
        if (rawSlot == layout.nextPageSlot()) {
            openPage(player, holder.kind(), holder.page() + 1, false);
            return;
        }
        if (rawSlot == layout.clearSelectionSlot()) {
            handleClear(player, holder.kind());
            return;
        }

        final int contentIndex = layout.contentSlots().indexOf(rawSlot);
        if (contentIndex < 0) {
            return;
        }

        final boolean hideLocked = configService.config().selector().hideLockedEntries();
        final List<SelectorService.Entry> entries = selectorService.entries(player.getUniqueId(), holder.kind(), hideLocked);
        final int perPage = Math.max(1, layout.contentSlots().size());
        final int index = holder.page() * perPage + contentIndex;
        if (index >= entries.size()) {
            sendMessage(player, "selector.unknown-entry");
            openPage(player, holder.kind(), holder.page(), false);
            return;
        }
        handleSelect(player, holder.kind(), entries.get(index));
    }

    private void handleSelect(final Player player, final CosmeticKind kind, final SelectorService.Entry entry) {
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
                    openPage(player, kind, 0, false);
                }));
    }

    private void handleClear(final Player player, final CosmeticKind kind) {
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
            openPage(player, kind, 0, false);
        }));
    }

    private void fillBackground(final Inventory inventory, final MenuLayoutConfig layout) {
        final Material filler = matchOrDefault(layout.fillerMaterial(), Material.BLACK_STAINED_GLASS_PANE);
        final ItemStack fillerItem = new ItemStack(filler);
        final ItemMeta meta = fillerItem.getItemMeta();
        meta.displayName(Component.empty());
        fillerItem.setItemMeta(meta);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, fillerItem);
        }
    }

    private void populateEntries(
            final Inventory inventory,
            final MenuLayoutConfig layout,
            final List<SelectorService.Entry> entries,
            final int page,
            final int perPage,
            final Player player) {
        final int start = page * perPage;
        for (int index = 0; index < layout.contentSlots().size(); index++) {
            final int entryIndex = start + index;
            if (entryIndex >= entries.size()) {
                inventory.setItem(layout.contentSlots().get(index), null);
                continue;
            }
            inventory.setItem(layout.contentSlots().get(index), buildEntryItem(entries.get(entryIndex), layout));
        }
    }

    private ItemStack buildEntryItem(final SelectorService.Entry entry, final MenuLayoutConfig layout) {
        final Material material = matchOrDefault(
                entry.owned() ? layout.entryMaterial() : layout.lockedMaterial(), Material.PAPER);
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        Component name = entry.cosmetic().prefix().rendered();
        if (entry.active()) {
            name = name.decorate(TextDecoration.BOLD);
        }
        meta.displayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private void populateNavigation(final Inventory inventory, final MenuLayoutConfig layout, final int page, final int totalPages) {
        if (page > 0) {
            inventory.setItem(layout.previousPageSlot(), namedItem(Material.ARROW, Component.text("Previous page")));
        }
        if (page < totalPages - 1) {
            inventory.setItem(layout.nextPageSlot(), namedItem(Material.ARROW, Component.text("Next page")));
        }
    }

    private void populateClear(
            final Inventory inventory,
            final MenuLayoutConfig layout,
            final CosmeticKind kind,
            final Player player,
            final boolean alwaysShow) {
        if (alwaysShow || selectorService.hasActiveSelection(player.getUniqueId(), kind)) {
            inventory.setItem(layout.clearSelectionSlot(), namedItem(Material.BARRIER, Component.text("Clear selection")));
        }
    }

    private ItemStack namedItem(final Material material, final Component name) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private Material matchOrDefault(final String name, final Material fallback) {
        final Material material = Material.matchMaterial(name);
        return material != null ? material : fallback;
    }

    private Component kindComponent(final CosmeticKind kind) {
        return Component.text(kind.name().toLowerCase(Locale.ROOT));
    }

    private void sendMessage(final Player player, final String key, final net.kyori.adventure.text.minimessage.tag.resolver.TagResolver... placeholders) {
        configService.messages().render(key, placeholders).ifPresent(player::sendMessage);
    }
}
