package io.github.viniciussambinello.stags.infrastructure.selector;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import io.github.viniciussambinello.stags.domain.cosmetic.CosmeticKind;

final class MenuHolder implements InventoryHolder {

    private final UUID playerId;
    private final CosmeticKind kind;
    private final int page;
    private final AtomicReference<Inventory> inventory;

    MenuHolder(final UUID playerId, final CosmeticKind kind, final int page) {
        this.playerId = playerId;
        this.kind = kind;
        this.page = page;
        this.inventory = new AtomicReference<>();
    }

    UUID playerId() {
        return playerId;
    }

    CosmeticKind kind() {
        return kind;
    }

    int page() {
        return page;
    }

    void attach(final Inventory inventory) {
        this.inventory.set(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory.get();
    }
}
