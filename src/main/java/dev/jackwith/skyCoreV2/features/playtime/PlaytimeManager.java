package dev.jackwith.skyCoreV2.features.playtime;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.database.AnalyticsCollection;
import dev.jackwith.skyCoreV2.features.playtime.listeners.PlaytimeListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class PlaytimeManager {

    private final SkyCore plugin;
    private final PlaytimeCache cache;
    private final PlaytimeListener listener;
    private BukkitTask saveTask;

    public PlaytimeManager(SkyCore plugin) {
        this.plugin = plugin;
        this.cache = new PlaytimeCache();
        this.listener = new PlaytimeListener(plugin, cache);
    }

    public void enable() {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        this.saveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                listener.savePlayer(player.getUniqueId());
            }
        }, 1200L, 1200L);
    }

    public void disable() {
        if (saveTask != null) saveTask.cancel();

        for (Player player : Bukkit.getOnlinePlayers()) {
            listener.savePlayer(player.getUniqueId());
        }
    }

    public AnalyticsCollection getDatabase() { return SkyCore.getAnalyticsCollection(); }

    public PlaytimeCache getCache() { return cache; }
}