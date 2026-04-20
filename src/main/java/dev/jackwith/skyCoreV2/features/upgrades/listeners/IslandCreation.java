package dev.jackwith.skyCoreV2.features.upgrades.listeners;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.database.UpgradesCollection;
import dev.jackwith.skyCoreV2.hooks.BentoBoxHook;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.island.IslandCreateEvent;
import world.bentobox.bentobox.api.events.island.IslandResetEvent;
import world.bentobox.bentobox.database.objects.Island;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class IslandCreation implements Listener {

    @EventHandler
    public void onIslandCreate(IslandCreateEvent event) {
        BentoBoxHook.applyBorder(event.getPlayerUUID());
        BentoBoxHook.setIslandSpawn(event.getPlayerUUID());

    }

    @EventHandler
    public void onIslandReset(IslandResetEvent event) {
        BentoBoxHook.applyBorder(event.getPlayerUUID());
        BentoBoxHook.setIslandSpawn(event.getPlayerUUID());
    }

    @EventHandler
    public static void setupBoxJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        UpgradesCollection Uc = SkyCore.getUpgradesCollection();

        Optional<Island> islandOpt = Optional.ofNullable(
                BentoBox.getInstance()
                        .getIslands()
                        .getIsland(Objects.requireNonNull(Bukkit.getWorld("boxed_world")), uuid)
        );

        if (islandOpt.isEmpty()) return;

        UUID ownerUuid = islandOpt.get().getOwner();
        if (ownerUuid == null) return;

        Uc.createPlayer(ownerUuid.toString());
        int ownerLevel = Uc.getLevel(ownerUuid.toString());

        ConfigurationSection levelData = SkyCore
                .getUpgradesConfig()
                .getConfigurationSection("upgrades." + ownerLevel);

        if (levelData != null) {
            BentoBoxHook.updateIslandSize(ownerUuid.toString(), levelData.getInt("size"), player);
        }
    }
}
