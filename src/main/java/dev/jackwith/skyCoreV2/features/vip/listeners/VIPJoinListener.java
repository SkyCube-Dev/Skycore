package dev.jackwith.skyCoreV2.features.vip.listeners;

import dev.jackwith.skyCoreV2.features.vip.VIPManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class VIPJoinListener implements Listener {

    private final VIPManager vipManager;

    public VIPJoinListener(VIPManager vipManager) {
        this.vipManager = vipManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String username = player.getName();

        // Check if database is loaded
        if (!vipManager.isDatabaseLoaded()) {
            return;
        }

        // Process VIP subscription (if active)
        vipManager.processPlayerJoin(username);
    }
}