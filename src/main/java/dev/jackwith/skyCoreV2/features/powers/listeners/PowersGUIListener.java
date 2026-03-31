package dev.jackwith.skyCoreV2.features.powers.listeners;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.features.powers.PowerManager;
import dev.jackwith.skyCoreV2.features.powers.gui.PowersGui;
import dev.jackwith.skyCoreV2.features.powers.gui.PowersHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PowersGUIListener implements Listener {

    private final SkyCore plugin;
    private final Map<Integer, String> slotToPower = new HashMap<>();
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final Component PREFIX = Component.text("(/powers) ", NamedTextColor.GRAY);

    public PowersGUIListener(SkyCore plugin) {
        this.plugin = plugin;
        mapSlots();
    }

    private void mapSlots() {
        ConfigurationSection powersSection = plugin.getPowersConfig().getConfigurationSection("powers");
        if (powersSection == null) return;

        for (String key : powersSection.getKeys(false)) {
            int slot = powersSection.getInt(key + ".layout", -1) - 1;
            if (slot >= 0) slotToPower.put(slot, key);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof PowersHolder)) return;

        e.setCancelled(true);
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

        if (!(e.getWhoClicked() instanceof Player player)) return;

        int slot = e.getRawSlot();
        if (slot >= e.getInventory().getSize()) return;

        if (slot == 40) {
            player.closeInventory();
            return;
        }
        if (slot >= 36) return;

        String powerKey = slotToPower.get(slot);
        if (powerKey == null) return;

        handlePower(player, powerKey);
        refreshGUI(player);
    }

    private void handlePower(Player player, String powerKey) {
        UUID uuid = player.getUniqueId();
        var db = SkyCore.getPowersCollection();

        String current = db.getPower(uuid);
        boolean owned = db.isPowerOwned(uuid, powerKey);
        boolean freeUsed = db.hasUsedFreePower(uuid);

        String rawName = SkyCore.getPowersConfig().getString("powers." + powerKey + ".name", powerKey);
        Component displayName = MINI.deserialize(rawName);

        if (powerKey.equalsIgnoreCase(current)) {
            db.setPower(uuid, "none");
            PowerManager.applyPower(player, "none");
            sendFeedback(player, "Unequipped ", displayName, NamedTextColor.GRAY, Sound.BLOCK_NOTE_BLOCK_PLING, 1.2f);
            return;
        }

        if (owned) {
            db.setPower(uuid, powerKey);
            PowerManager.applyPower(player, powerKey);
            sendFeedback(player, "Re-equipped ", displayName, NamedTextColor.GREEN, Sound.ENTITY_PLAYER_LEVELUP, 1.2f);
            return;
        }

        if (!freeUsed) {
            db.setPowerOwned(uuid, powerKey);
            db.setPower(uuid, powerKey);
            PowerManager.applyPower(player, powerKey);
            sendFeedback(player, "Equipped ", displayName, NamedTextColor.GREEN, Sound.ENTITY_PLAYER_LEVELUP, 1.5f);
            return;
        }

        int balance = plugin.getPpAPI().look(uuid);
        int cost = 300;
        if (balance < cost) {
            player.sendMessage(PREFIX.append(Component.text("You need 300⛃ to switch powers!", NamedTextColor.RED)));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            return;
        }

        plugin.getPpAPI().take(uuid, cost);
        db.setPowerOwned(uuid, powerKey);
        db.setPower(uuid, powerKey);
        PowerManager.applyPower(player, powerKey);

        player.sendMessage(PREFIX.append(Component.text("Switched to ", NamedTextColor.GRAY))
                .append(displayName)
                .append(Component.text(" and deleted previous power.", NamedTextColor.GRAY)));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 1.5f);
    }

    private void refreshGUI(Player player) {
        InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
        if (!(holder instanceof PowersHolder powersHolder)) return;

        PowersGui gui = powersHolder.getGui();
        if (gui != null) gui.refresh();
    }

    private void sendFeedback(Player player, String msg, Component powerName, NamedTextColor color, Sound sound, float pitch) {
        player.sendMessage(PREFIX.append(Component.text(msg, color)).append(powerName));
        player.playSound(player.getLocation(), sound, 1f, pitch);
    }
}