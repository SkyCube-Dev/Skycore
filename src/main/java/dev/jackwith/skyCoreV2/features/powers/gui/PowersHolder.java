package dev.jackwith.skyCoreV2.features.powers.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.NonNull;

public class PowersHolder implements InventoryHolder {
    private PowersGui gui;

    public PowersGui getGui() {
        return this.gui;
    }

    public void setGui(PowersGui gui) {
        this.gui = gui;
    }

    public @NonNull Inventory getInventory() {
        return null;
    }
}
