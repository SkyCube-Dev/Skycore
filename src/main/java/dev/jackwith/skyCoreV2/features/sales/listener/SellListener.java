package dev.jackwith.skyCoreV2.features.sales.listener;

import dev.jackwith.skyCoreV2.features.sales.gui.SellGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class SellListener implements Listener {

    @EventHandler
    public void onGuiClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof SellGui gui) {

            Player player = (Player) event.getPlayer();
            gui.processSale(player);
        }
    }
}