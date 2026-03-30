package dev.jackwith.skyCoreV2.registeries;

import dev.jackwith.skyCoreV2.SkyCore;
import org.bukkit.event.Listener;

public class ListenerRegistry {
    private final SkyCore plugin;

    public ListenerRegistry(SkyCore plugin) {
        this.plugin = plugin;
    }

    public void registerAll(Listener... listeners) {
        for (Listener listener : listeners) {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }
}