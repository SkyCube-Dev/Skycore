package dev.jackwith.skyCoreV2.features.upgrades.gui;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.database.UpgradesCollection;
import dev.jackwith.skyCoreV2.hooks.BentoBoxHook;
import dev.jackwith.skyCoreV2.utils.TimeF;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UpgradesGui {

    private final SkyCore plugin = SkyCore.getInstance();
    private final MiniMessage mm = MiniMessage.miniMessage();

    public void open(Player player, UUID ownerUuid) {
        UpgradesHolder holder = new UpgradesHolder(ownerUuid);
        Inventory inv = Bukkit.createInventory(holder, 54, mm.deserialize("<dark_gray>Box Upgrades"));

        holder.setInventory(inv);

        updateInventory(player, inv, ownerUuid);
        player.openInventory(inv);

        startRefreshTask(player, inv, ownerUuid);
    }

    private void updateInventory(Player player, Inventory inv, UUID ownerUuid) {
        ItemStack background = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta backgroundMeta = background.getItemMeta();
        if (backgroundMeta != null) {
            backgroundMeta.displayName(mm.deserialize(" "));
            background.setItemMeta(backgroundMeta);
        }

        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 34 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, background);
            }
        }

        ConfigurationSection upgrades = SkyCore.getUpgradesConfig().getConfigurationSection("upgrades");
        if (upgrades == null) return;

        UpgradesCollection col = SkyCore.getUpgradesCollection();

        int currentLevel = col.getLevel(ownerUuid.toString());
        long upgradingTimer = col.getUpgradingUntil(ownerUuid.toString());
        long now = System.currentTimeMillis();

        if (upgradingTimer > 0 && upgradingTimer <= now) {
            col.updateDocument(ownerUuid.toString(), currentLevel + 1, 0);

            currentLevel++;
            upgradingTimer = 0;

            ConfigurationSection nextData = upgrades.getConfigurationSection(String.valueOf(currentLevel));
            if (nextData != null) {
                BentoBoxHook.updateIslandSize(ownerUuid.toString(), nextData.getInt("size"), player);
            }
        }

        boolean isUpgrading = upgradingTimer > now;

        for (String key : upgrades.getKeys(false)) {
            ConfigurationSection levelData = upgrades.getConfigurationSection(key);
            if (levelData == null) continue;

            try {
                int level = Integer.parseInt(key);
                int slot = levelData.getInt("slot");
                int size = levelData.getInt("size");
                double price = levelData.getDouble("price");
                int exp = levelData.getInt("exp");

                long timeLeft = isUpgrading ? (upgradingTimer - now) / 1000 : levelData.getInt("time");

                boolean isVip = levelData.getBoolean("vip", false);
                boolean hasVip = player.hasPermission("skycore.vip");

                boolean isNext = level == currentLevel + 1;

                String buttonType;

                if (level <= currentLevel) {
                    buttonType = "upgraded_button";

                } else if (isUpgrading && isNext) {
                    buttonType = "upgrading_button";

                } else if (isVip) {
                    if (!hasVip) {
                        buttonType = "no_vip_prevs_upgrade_button";
                    } else if (isNext) {
                        buttonType = "vip_upgrade_button";
                    } else {
                        buttonType = "vip_prevs_upgrade_button";
                    }

                } else if (isNext) {
                    buttonType = "upgrade_button";

                } else {
                    buttonType = "prevs_upgrade_button";
                }

                inv.setItem(slot, createUpgradeItem(buttonType, level, size, price, exp, timeLeft));
            } catch (NumberFormatException ignored) {}
        }
    }

    private void startRefreshTask(Player player, Inventory inv, UUID ownerUuid) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !(player.getOpenInventory().getTopInventory().getHolder() instanceof UpgradesHolder)) {
                    this.cancel();
                    return;
                }
                updateInventory(player, inv, ownerUuid);
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private ItemStack createUpgradeItem(String type, int level, int size, double price, int exp, long timeLeft) {
        ConfigurationSection btnSection = SkyCore.getUpgradesConfig().getConfigurationSection("buttons." + type);

        if (btnSection == null) return new ItemStack(Material.STONE);

        Material mat = Material.valueOf(btnSection.getString("material", "STONE"));
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(mm.deserialize("<!italic>" + btnSection.getString("name", "")
                .replace("<level>", String.valueOf(level))));

        List<String> rawLore = btnSection.getStringList("lore");
        List<Component> lore = new ArrayList<>();

        String formattedTime = TimeF.formatTime(timeLeft);

        for (String line : rawLore) {
            String formattedLine = line
                    .replace("<level>", String.valueOf(level))
                    .replace("<size>", String.valueOf(size))
                    .replace("<price>", String.format("%,.0f", price))
                    .replace("<exp>", String.valueOf(exp))
                    .replace("<time>", formattedTime);
            lore.add(mm.deserialize("<!italic>" + formattedLine));
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }
}