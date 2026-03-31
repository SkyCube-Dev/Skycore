package dev.jackwith.skyCoreV2.features.analytics.listeners;

import dev.jackwith.skyCoreV2.SkyCore;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class LoggerListener implements Listener {
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onChat(AsyncChatEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        SkyCore.getAnalyticsCollection().logAction(uuid, "CHAT", message);
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        String command = event.getMessage();
        SkyCore.getAnalyticsCollection().logAction(uuid, "COMMAND", command);
    }
}