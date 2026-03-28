package dev.jackwith.skyCoreV2.features.powers.listeners;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.databases.AnalyticsDB;
import dev.jackwith.skyCoreV2.features.powers.PowerManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PowerEffectsListener implements Listener {

    private final SkyCore plugin = SkyCore.getInstance();
    private final String META = "TEMP_POWER_BLOCK";
    private final String PENDING = "REMOVAL_PENDING";
    private final String HULK = "HULK_PROCESSING";
    private final Map<UUID, Location> lastPlatform = new HashMap<>();

    public void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String power = plugin.getPowerDB().getPower(p.getUniqueId());
                    if (power == null || power.equalsIgnoreCase("none")) continue;

                    switch (power.toLowerCase()) {
                        case "frozone" -> handlePlatform(p, Material.WATER, Material.ICE, 3);
                        case "inferno" -> handlePlatform(p, Material.LAVA, Material.MAGMA_BLOCK, 3);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        UUID uuid = e.getUniqueId();
        String name = e.getName();
        String ip = e.getAddress().getHostAddress();
        String host = e.getHostname().split(":")[0];

        AnalyticsDB db = plugin.getAnalyticsDB();
        if (db.checkJoin(uuid.toString(), name, ip, host)) {
            String count = String.format("%,d", db.totalPlayerCount());
            String msg = "<white>:welcome: <green>" + name + "</green> <white>ʜᴀꜱ ᴊᴏɪɴᴇᴅ ꜰᴏʀ ᴛʜᴇ ꜰɪʀꜱᴛ ᴛɪᴍᴇ</white> <green>[#" + count + "]</green>";
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcast(MiniMessage.miniMessage().deserialize(msg)));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.getPowerDB().loadUserData(p.getUniqueId());
        PowerManager.applyPower(p, plugin.getPowerDB().getPower(p.getUniqueId()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (b.hasMetadata(META)) {
            e.setCancelled(true);
            return;
        }

        Player p = e.getPlayer();
        String power = plugin.getPowerDB().getPower(p.getUniqueId());
        if (!"hulk".equalsIgnoreCase(power) || b.hasMetadata(HULK)) return;

        if (!p.getInventory().getItemInMainHand().getType().name().contains("PICKAXE")) return;

        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && y == 0 && z == 0) continue;
            Block rel = b.getRelative(x, y, z);
            if (rel.getType().isAir() || rel.getType() == Material.BEDROCK || rel.hasMetadata(META)) continue;

            rel.setMetadata(HULK, new FixedMetadataValue(plugin, true));
            rel.breakNaturally(p.getInventory().getItemInMainHand());
            rel.removeMetadata(HULK, plugin);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        plugin.getPowerDB().unloadUserData(id);
        lastPlatform.remove(id);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        Player p = e.getPlayer();
        PowerManager.applyPower(p, plugin.getPowerDB().getPower(p.getUniqueId()));
    }

    private void handlePlatform(Player p, Material fluid, Material blockType, int radius) {
        Location loc = p.getLocation();
        UUID id = p.getUniqueId();
        Location last = lastPlatform.get(id);
        if (last != null && last.getBlockX() == loc.getBlockX() && last.getBlockY() == loc.getBlockY() && last.getBlockZ() == loc.getBlockZ()) return;

        lastPlatform.put(id, loc.clone());

        for (int y = -1; y >= -2; y--)
            for (int x = -radius; x <= radius; x++)
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z > radius * radius) continue;
                    Block b = loc.getWorld().getBlockAt(loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);
                    if (b.getType() != fluid || b.getRelative(0, 1, 0).getType().isSolid()) continue;

                    BlockData original = b.getBlockData();
                    b.setType(blockType, false);
                    b.setMetadata(META, new FixedMetadataValue(plugin, original));
                    scheduleRemoval(b);
                }
    }

    private void scheduleRemoval(Block b) {
        if (b.hasMetadata(PENDING)) return;

        b.setMetadata(PENDING, new FixedMetadataValue(plugin, true));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!b.hasMetadata(META)) return;

            boolean nearby = Bukkit.getOnlinePlayers().stream()
                    .anyMatch(p -> p.getWorld().equals(b.getWorld()) &&
                            p.getLocation().distanceSquared(b.getLocation().add(0.5, 1, 0.5)) < 2);

            if (nearby) {
                b.removeMetadata(PENDING, plugin);
                scheduleRemoval(b);
            } else {
                Object data = b.getMetadata(META).get(0).value();
                if (data instanceof BlockData original) b.setBlockData(original, false);
                b.removeMetadata(META, plugin);
                b.removeMetadata(PENDING, plugin);
            }
        }, 30L);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;

        String power = plugin.getPowerDB().getPower(p.getUniqueId());
        if ("inferno".equalsIgnoreCase(power) && (e.getCause() == EntityDamageEvent.DamageCause.HOT_FLOOR ||
                e.getCause() == EntityDamageEvent.DamageCause.LAVA ||
                e.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK)) e.setCancelled(true);

        if (e.getCause() == EntityDamageEvent.DamageCause.FALL && ("spiderman".equalsIgnoreCase(power) || "superman".equalsIgnoreCase(power)))
            e.setCancelled(true);
    }

    @EventHandler
    public void onPiston(BlockPistonExtendEvent e) {
        if (e.getBlocks().stream().anyMatch(b -> b.hasMetadata(META))) e.setCancelled(true);
    }
}