package dev.jackwith.skyCoreV2.hooks;

import dev.jackwith.skyCoreV2.SkyCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;

import java.util.Optional;
import java.util.UUID;

public class BentoBoxHook {

    public static SkyCore plugin = null;

    {
        new SkyCore();
        plugin = SkyCore.getInstance();
    }

    public static void setIslandSpawn(UUID ownerUuid) {
        if (ownerUuid == null) return;

        Player player = Bukkit.getPlayer(ownerUuid);
        if (player == null) return;

        Optional<Island> islandOpt = Optional.ofNullable(
                BentoBox.getInstance().getIslands().getIsland(player.getWorld(), ownerUuid)
        );

        islandOpt.ifPresent(island -> {
            Location spawn = island.getCenter();

            spawn.setY(98);
            island.setCenter(spawn);
        });
    }

    public static void applyCurrentBorder(UUID uuid) {
        if (uuid == null) return;

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        int level = plugin.getUpgradeDB().getLevel(uuid);

        if (level < 0) {
            level = 0;
        }

        int currentSize = plugin.getUpgradesConfig().getInt("upgrades." + level + ".size", 8);
        plugin.updateIslandSize(uuid.toString(), currentSize, player);
    }
}
