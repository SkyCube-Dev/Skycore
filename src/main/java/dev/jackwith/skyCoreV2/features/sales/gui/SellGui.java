package dev.jackwith.skyCoreV2.features.sales.gui;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.features.sales.SaleService;
import dev.jackwith.skyCoreV2.hooks.SoldItem;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.*;

public class SellGui implements InventoryHolder {

    private final Inventory inventory;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public SellGui() {
        this.inventory = Bukkit.createInventory(this, 54, mm.deserialize("Sell GUI"));
    }

    @Override
    public @NonNull Inventory getInventory() {
        return inventory;
    }

    public void processSale(Player player) {

        SaleService saleService = new SaleService();
        SaleService.SaleResult result =
                saleService.processInventory(player, inventory.getContents());

        if (result.getBaseTotal() <= 0) {
            player.sendMessage(mm.deserialize("<red>Nothing to sell."));
            return;
        }

        inventory.clear();
        SkyCore.getInstance().getEconomy().depositPlayer(player, result.getFinalTotal());

        sendFancyReceipt(
                player,
                result.getFinalTotal(),
                result.getSoldItems(),
                result.getBoostPercent()
        );
    }

    private void sendFancyReceipt(Player player,
                                  double finalPayout,
                                  Map<String, SoldItem> soldItems,
                                  double boostPercent) {

        StringBuilder hover = new StringBuilder("<white><u>Sell Receipt</u>");

        String name;
        SoldItem data = null;

        for (Map.Entry<String, SoldItem> entry : soldItems.entrySet()) {

            name = entry.getKey();
            data = entry.getValue();

            hover.append("<br><green>")
                    .append(data.getAmount())
                    .append("x ")
                    .append(name)
                    .append(" <white>for <green>$")
                    .append(String.format("%.2f", data.getTotalPrice()));
        }

        if (boostPercent > 0) {
            hover.append("<br><br><aqua>+")
                    .append((int) boostPercent)
                    .append("% Pet Boost Applied");
        }

        String titleText = "<green><b>+$" + String.format("%.2f", finalPayout);
        assert data != null;
        String subtitleText = "<gray>You sold <green><u>" + data.getAmount() + "<reset><gray> items in this batch";

        Title title = Title.title(
                mm.deserialize(titleText),
                mm.deserialize(subtitleText),
                Title.Times.times(
                        Duration.ofMillis(300),
                        Duration.ofSeconds(3),
                        Duration.ofMillis(500)
                )
        );

        player.showTitle(title);
        String joinedNames = String.join(", ", soldItems.keySet());
        if (joinedNames.length() > 40) {
            joinedNames = joinedNames.substring(0, 37) + "...";
        }

        String message =
                "<gray>(/sellgui) " +
                        "<white>You sold <green>" + joinedNames +
                        " <white>for <green>$" + String.format("%.2f", finalPayout) +
                        " <gray><hover:show_text:\"" + hover + "\">[RECEIPT]</hover>";

        player.sendMessage(mm.deserialize(message));
    }

//    private void returnItem(Player player, ItemStack item) {
//        HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(item);
//        for (ItemStack leftOver : remaining.values()) {
//            player.getWorld().dropItemNaturally(player.getLocation(), leftOver);
//        }
//    }
}