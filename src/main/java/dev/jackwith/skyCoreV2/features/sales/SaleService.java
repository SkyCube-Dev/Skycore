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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class SaleService {

    public static class SaleResult {

        private final double baseTotal;
        private final double finalTotal;
        private final double boostPercent;
        private final Map<String, SoldItem> soldItems;

        public SaleResult(double baseTotal,
                          double finalTotal,
                          double boostPercent,
                          Map<String, SoldItem> soldItems) {
            this.baseTotal    = baseTotal;
            this.finalTotal   = finalTotal;
            this.boostPercent = boostPercent;
            this.soldItems    = soldItems;
        }

        public double getBaseTotal()             { return baseTotal; }
        public double getFinalTotal()            { return finalTotal; }
        public double getBoostPercent()          { return boostPercent; }
        public Map<String, SoldItem> getSoldItems() { return soldItems; }
    }

    public SaleResult processInventory(Player player, ItemStack[] contents) {
        if (!Bukkit.getPluginManager().isPluginEnabled("ShopGUIPlus")) {
            return new SaleResult(0, 0, 0, new LinkedHashMap<>());
        }

        UUID uuid = player.getUniqueId();

        double playerBoostPercent = (SkyCore.getInstance().getsalesDB().getPlayerSellBoost(uuid) - 1.0) * 100.0;
        double petBoostPercent    = calculatePetBoost(uuid);
        double totalBoostPercent  = playerBoostPercent + petBoostPercent;
        double multiplier         = 1.0 + (totalBoostPercent / 100.0);

        double totalBasePrice = 0;
        Map<String, SoldItem> soldItems = new LinkedHashMap<>();

        for (ItemStack item : contents) {
            if (item == null || item.getType() == Material.AIR) continue;

            var shopItem = ShopGuiPlusApi.getItemStackShopItem(item);
            if (shopItem == null) continue;

            double price = ShopGuiPlusApi.getItemStackPriceSell(item);
            if (price <= 0) continue;

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

            SkyCore.getInstance().getsalesDB().logSale(
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
        return new SaleResult(totalBasePrice, finalTotal, totalBoostPercent, soldItems);
    }

    private double calculatePetBoost(UUID uuid) {
        PetService service = SkyCore.getInstance().getPetService();
        double totalBoost = 0;

        for (String id : service.getEquippedPets(uuid)) {
            Pet pet = SkyCore.getInstance().getPetService().getPet(id);
            if (pet != null) {
                totalBoost += pet.getBoost();
            }
        }

        return totalBoost;
    }
}