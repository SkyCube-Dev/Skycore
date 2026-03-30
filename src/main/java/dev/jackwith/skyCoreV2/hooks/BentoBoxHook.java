package dev.jackwith.skyCoreV2.hooks;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.database.UpgradesCollection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;

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

    public static void updateIslandSize(String ownerUuid, int newSize, Player actor) {
        UUID targetUuid = UUID.fromString(ownerUuid);
        World boxedWorld = Bukkit.getWorld("boxed_world");

        if (boxedWorld == null) return;

        IslandsManager manager = BentoBox.getInstance().getIslands();
        Island island = manager.getIsland(boxedWorld, targetUuid);

        if (island == null) return;

        int radius = newSize / 2;
        int oldRange = island.getProtectionRange();

        island.setProtectionRange(radius);

        try {
            var islandEvent = world.bentobox.bentobox.api.events.island.IslandEvent.builder()
                    .island(island)
                    .location(island.getCenter())
                    .reason(world.bentobox.bentobox.api.events.island.IslandEvent.Reason.RANGE_CHANGE)
                    .involvedPlayer(actor != null ? actor.getUniqueId() : targetUuid)
                    .admin(true)
                    .protectionRange(radius, oldRange)
                    .build();

            Bukkit.getPluginManager().callEvent(islandEvent);
        } catch (Exception ignored) {
        }

        String message = "§b§lSKYCORE §8» §fIsland protection expanded to §b" + newSize + "x" + newSize + "§f!";
        island.getMemberSet().forEach(memberUuid -> {
            Player p = Bukkit.getPlayer(memberUuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        });
    }

    public static UUID getIslandOwnerUUID(Player player) {
        Optional<Island> island = Optional.ofNullable(BentoBox.getInstance().getIslands().getIsland(player.getWorld(), player.getUniqueId()));

        if (island.isPresent()) {
            return island.get().getOwner();
        }

        return player.getUniqueId();
    }

    public static void applyBorder(UUID uuid) {
        if (uuid == null) return;

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        int level = new UpgradesCollection().getLevel(String.valueOf(uuid));

        if (level < 0) {
            level = 0;
        }

        int currentSize = SkyCore.getUpgradesConfig().getInt("upgrades." + level + ".size", 8);
        updateIslandSize(uuid.toString(), currentSize, player);
    }
}