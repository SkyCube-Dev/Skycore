package dev.jackwith.skyCoreV2.features.upgrades.listeners;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.hooks.BentoBoxHook;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.island.IslandCreateEvent;
import world.bentobox.bentobox.api.events.island.IslandResetEvent;
import world.bentobox.bentobox.database.objects.Island;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class IslandCreateListener implements Listener {

    private final SkyCore plugin;

    {
        plugin = SkyCore.getInstance();
    }

    @EventHandler
    public void onIslandCreate(IslandCreateEvent event) {
        BentoBoxHook.applyCurrentBorder(event.getPlayerUUID());
        BentoBoxHook.setIslandSpawn(event.getPlayerUUID());

    }

    @EventHandler
    public void onIslandReset(IslandResetEvent event) {
        BentoBoxHook.applyCurrentBorder(event.getPlayerUUID());
        BentoBoxHook.setIslandSpawn(event.getPlayerUUID());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        SkyCore.getInstance().getUpgradeDB().unloadUserData(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        SkyCore.getInstance().getUpgradeDB().loadUserData(event.getPlayer().getUniqueId());

        Player player = event.getPlayer();
        Optional<Island> islandOpt = Optional.ofNullable(BentoBox.getInstance().getIslands().getIsland(Objects.requireNonNull(Bukkit.getWorld("boxed_world")), player.getUniqueId()));

        if (islandOpt.isPresent()) {
            UUID ownerUuid = islandOpt.get().getOwner();

            int currentLevel = plugin.getUpgradeDB().getLevel(ownerUuid);
            ConfigurationSection levelData = plugin.getUpgradesConfig().getConfigurationSection("upgrades." + currentLevel);

            if (levelData != null) {
                assert ownerUuid != null;
                plugin.updateIslandSize(ownerUuid.toString(), levelData.getInt("size"), player);
            }
        }
    }
}