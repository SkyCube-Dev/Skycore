package dev.jackwith.skyCoreV2.features.upgrades.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class UpgradesHolder implements InventoryHolder {

    private final UUID ownerUuid;
    private Inventory inventory;

    public UpgradesHolder(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}