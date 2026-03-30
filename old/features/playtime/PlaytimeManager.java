package dev.jackwith.skyCoreV2.features.playtime;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.databases.AnalyticsDB;
import dev.jackwith.skyCoreV2.features.playtime.listeners.PlaytimeListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class PlaytimeManager {

    private final SkyCore plugin;
    private final AnalyticsDB database;
    private final PlaytimeCache cache;
    private final PlaytimeListener listener;
    private BukkitTask saveTask;

    public PlaytimeManager(SkyCore plugin) {
        this.plugin = plugin;
        this.database = new AnalyticsDB();
        this.cache = new PlaytimeCache();
        this.listener = new PlaytimeListener(plugin, cache, database);
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

    public AnalyticsDB getDatabase() {
        return database;
    }
    public PlaytimeCache getCache() { return cache; }
}