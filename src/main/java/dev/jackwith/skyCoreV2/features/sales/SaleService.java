package dev.jackwith.skyCoreV2.features.sales;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.features.pets.Pet;
import dev.jackwith.skyCoreV2.features.pets.PetService;
import dev.jackwith.skyCoreV2.hooks.SoldItem;
import net.brcdev.shopgui.ShopGuiPlusApi;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SaleService {

    public static class SaleResult {

        private final double baseTotal;
        private final double finalTotal;
        private final double boostPercent;
        private final Map<String, SoldItem> soldItems;
        private final List<ItemStack> unsoldItems;

        public SaleResult(double baseTotal,
                          double finalTotal,
                          double boostPercent,
                          Map<String, SoldItem> soldItems,
                          List<ItemStack> unsoldItems) {
            this.baseTotal    = baseTotal;
            this.finalTotal   = finalTotal;
            this.boostPercent = boostPercent;
            this.soldItems    = soldItems;
            this.unsoldItems  = unsoldItems;
        }

        public double getBaseTotal()                { return baseTotal; }
        public double getFinalTotal()               { return finalTotal; }
        public double getBoostPercent()             { return boostPercent; }
        public Map<String, SoldItem> getSoldItems() { return soldItems; }
        public List<ItemStack> getUnsoldItems()     { return unsoldItems; }  // NEW
    }

    private double getMaxPrestigeBoost(Player player) {
        double max = 0;

        if (player.hasPermission("prestige.level.6")) {
            max = Math.max(max, 50);
        }
        if (player.hasPermission("prestige.level.5")) {
            max = Math.max(max, 40);
        }
        if (player.hasPermission("prestige.level.4")) {
            max = Math.max(max, 30);
        }
        if (player.hasPermission("prestige.level.3")) {
            max = Math.max(max, 20);
        }
        if (player.hasPermission("prestige.level.2")) {
            max = Math.max(max, 10);
        }

        return max;
    }

    public SaleResult processInventory(Player player, ItemStack[] contents) {
        if (!Bukkit.getPluginManager().isPluginEnabled("ShopGUIPlus")) {
            return new SaleResult(0, 0, 0, new LinkedHashMap<>(), List.of());
        }

        UUID uuid = player.getUniqueId();

        double baseBoostPercent = (SkyCore.getSalesCollection().getPlayerSellBoost(uuid) - 1.0) * 100.0;
        double prestigeBoost    = getMaxPrestigeBoost(player);
        double playerBoostPercent = baseBoostPercent + prestigeBoost;

        double petBoostPercent    = calculatePetBoost(uuid);
        double totalBoostPercent  = playerBoostPercent + petBoostPercent;
        double multiplier         = 1.0 + (totalBoostPercent / 100.0);

        double totalBasePrice = 0;
        Map<String, SoldItem> soldItems = new LinkedHashMap<>();
        List<ItemStack> unsoldItems     = new ArrayList<>();

        for (ItemStack item : contents) {
            if (item == null || item.getType() == Material.AIR) continue;

            var shopItem = ShopGuiPlusApi.getItemStackShopItem(item);
            if (shopItem == null) {
                unsoldItems.add(item);
                continue;
            }
            double price = ShopGuiPlusApi.getItemStackPriceSell(item);
            if (price <= 0) {
                unsoldItems.add(item);
                continue;
            }
            totalBasePrice += price;

            String shopId   = shopItem.getShop().getId();
            String itemId   = shopItem.getId();
            String material = item.getType().name();
            int amount      = item.getAmount();

            String rawName = item.getType()
                    .toString()
                    .replace("_", " ")
                    .toLowerCase();

            soldItems.computeIfAbsent(rawName, k -> new SoldItem())
                    .add(amount, price);

            SkyCore.getSalesCollection().logSale(
                    uuid,
                    shopId,
                    itemId,
                    material,
                    amount,
                    price,
                    price * multiplier
            );
        }

        double finalTotal = totalBasePrice * multiplier;
        return new SaleResult(totalBasePrice, finalTotal, totalBoostPercent, soldItems, unsoldItems);
    }

    private double calculatePetBoost(UUID uuid) {
        PetService service = SkyCore.getPetService();
        double totalBoost = 0;

        for (String id : service.getEquippedPets(uuid)) {
            Pet pet = SkyCore.getPetService().getPet(id);
            if (pet != null) {
                totalBoost += pet.boost();
            }
        }

        return totalBoost;
    }
}