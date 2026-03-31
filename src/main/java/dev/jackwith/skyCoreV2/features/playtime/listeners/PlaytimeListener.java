package dev.jackwith.skyCoreV2.features.playtime.listeners;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.features.playtime.PlaytimeCache;
import dev.jackwith.skyCoreV2.utils.StackTrace;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlaytimeListener implements Listener {

    private final JavaPlugin plugin;
    private final PlaytimeCache cache;
    private final Map<UUID, Long> moveCooldown = new HashMap<>();

    public PlaytimeListener(JavaPlugin plugin, PlaytimeCache cache) {
        this.plugin = plugin;
        this.cache = cache;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        long now = System.currentTimeMillis();

        if (moveCooldown.containsKey(uuid) && (now - moveCooldown.get(uuid) < 2000)) {
            return;
        }

        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {

            cache.updateActivity(uuid);
            moveCooldown.put(uuid, now);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        cache.startTracking(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        savePlayer(event.getPlayer().getUniqueId());
        cache.stopTracking(event.getPlayer().getUniqueId());
        moveCooldown.remove(event.getPlayer().getUniqueId());
    }

    public void savePlayer(UUID uuid) {
        long seconds = cache.getElapsedAndReset(uuid);
        if (seconds <= 0) return;

        boolean afk = cache.isAfk(uuid);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                SkyCore.getAnalyticsCollection().addPlaytime(String.valueOf(uuid), seconds, afk);
            } catch (Exception e) {
                StackTrace.error("Saving Player", e);
            }
        });
    }
}