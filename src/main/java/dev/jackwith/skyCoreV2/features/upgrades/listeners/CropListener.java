package dev.jackwith.skyCoreV2.features.upgrades.listeners;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.database.UpgradesCollection;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class CropListener implements Listener {

    private final SkyCore plugin;

    public CropListener(SkyCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void BlockGrow(BlockGrowEvent e) {
        if (CheckUpgrading(e.getBlock().getLocation())) {
            if (ThreadLocalRandom.current().nextBoolean()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void SpawnerSpawn(SpawnerSpawnEvent e) {
        assert e.getSpawner() != null;
        if (CheckUpgrading(e.getSpawner().getLocation())) {

            if (ThreadLocalRandom.current().nextBoolean()) {
                e.setCancelled(true);
            }
        }
    }

    private boolean CheckUpgrading(Location loc) {

        Optional<Island> islandOpt = BentoBox.getInstance()
                .getIslands()
                .getIslandAt(loc);

        if (islandOpt.isEmpty()) {
            return false;
        }

        Island island = islandOpt.get();
        UUID owner = island.getOwner();

        if (owner == null) {
            return false;
        }

        UpgradesCollection collection = new UpgradesCollection();

        long upgradingUntil = collection.getUpgradingUntil(owner.toString());
        return upgradingUntil > System.currentTimeMillis();
    }
}