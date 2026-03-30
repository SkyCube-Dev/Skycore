package dev.jackwith.skyCoreV2.features.upgrades.listeners;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.database.UpgradesCollection;
import dev.jackwith.skyCoreV2.features.upgrades.gui.UpgradesHolder;
import dev.jackwith.skyCoreV2.hooks.BentoBoxHook;
import dev.jackwith.skyCoreV2.utils.TimeF;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.UUID;

public class UpgradeListener implements Listener {

    private final SkyCore plugin;

    public UpgradeListener(SkyCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        if (!(e.getInventory().getHolder() instanceof UpgradesHolder holder)) return;

        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;

        UUID ownerUuid = holder.getOwnerUuid();

        UpgradesCollection upgradesCollection = SkyCore.getUpgradesCollection();

        int currentLevel = upgradesCollection.getLevel(ownerUuid.toString());
        long timeLeft = upgradesCollection.getUpgradingUntil(ownerUuid.toString());
        long now = System.currentTimeMillis();

        ConfigurationSection levelSection = SkyCore.getUpgradesConfig().getConfigurationSection("upgrades." + currentLevel);

        if (timeLeft > now) {
            int creditsRequired = levelSection.getInt("credits");
            double playerCredits = plugin.getPpAPI().look(player.getUniqueId());

            if (playerCredits >= creditsRequired) {
                plugin.getPpAPI().take(player.getUniqueId(), creditsRequired);

                int nextLevel = currentLevel + 1;
                ConfigurationSection nextLevelSection = SkyCore.getUpgradesConfig()
                        .getConfigurationSection("upgrades." + nextLevel);

                if (nextLevelSection == null) return;
                upgradesCollection.updateDocument(ownerUuid.toString(), nextLevel, 0);

                int newSize = nextLevelSection.getInt("size");

                BentoBoxHook.updateIslandSize(ownerUuid.toString(), newSize, player);

                player.sendMessage("§7(/upgrades) ♦ §fUpgrade instantly completed using §b"
                        + creditsRequired + " credits§f!");

            } else {
                player.sendMessage("§7(/upgrades) ♦ §cThis island is already being upgraded!");
            }
            return;
        }

        int nextLevel = currentLevel + 1;
        ConfigurationSection nextLevelSection = SkyCore.getUpgradesConfig().getConfigurationSection("upgrades." + nextLevel);

        if (nextLevelSection == null) return;

        if (e.getRawSlot() != nextLevelSection.getInt("slot")) return;

        double price = nextLevelSection.getDouble("price");
        int timeSeconds = nextLevelSection.getInt("time", 60);
        int requiredExp = nextLevelSection.getInt("exp");

        if (player.getLevel() < requiredExp) {
            player.sendMessage("§7(/upgrades) ♦ §cYou need level " + requiredExp + " to start this upgrade!");
            return;
        }

        if (!plugin.getEconomy().has(player, price)) {
            player.sendMessage("§7(/upgrades) ♦ §cYou don't have enough money! Cost: §e$" + String.format("%,.0f", price));
            return;
        }

        plugin.getEconomy().withdrawPlayer(player, price);
        long finishTime = now + (timeSeconds * 1000L);

        upgradesCollection.updateDocument(ownerUuid.toString(), currentLevel, finishTime);
        String formattedTime = TimeF.formatTime(timeSeconds);

        player.sendMessage("§7(/upgrades) ♦ §fYou started the island upgrade! the border will expand in §b" + formattedTime + "s§f.");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            upgradesCollection.updateDocument(ownerUuid.toString(), nextLevel, 0);

            int newSize = nextLevelSection.getInt("size");

            BentoBoxHook.updateIslandSize(ownerUuid.toString(), newSize, player);
        }, 20L * timeSeconds);
    }
}