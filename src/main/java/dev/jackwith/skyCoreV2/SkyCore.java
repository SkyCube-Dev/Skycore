package dev.jackwith.skyCoreV2;

import dev.jackwith.skyCoreV2.commands.UpgradesCommand;
import dev.jackwith.skyCoreV2.database.Database;
import dev.jackwith.skyCoreV2.features.upgrades.listeners.IslandCreation;
import dev.jackwith.skyCoreV2.registeries.CommandRegistry;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

public final class SkyCore extends JavaPlugin {

    private static SkyCore instance;

    private static FileConfiguration configuration;
    private static FileConfiguration upgrades;
    private static FileConfiguration lang;
    private static Database database;

    public SkyCore() {
        instance = this;
    }

    @Override
    public void onEnable() {
       loadConfigs();

       database = new Database();

        CommandRegistry commandRegistry = new CommandRegistry(this);
        commandRegistry.registerAll(
                new UpgradesCommand()
        );

        getServer().getPluginManager().registerEvents(new IslandCreation(), this);
    }

    @Override
    public void onDisable() {
        database.disconnect();
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

    public UUID getIslandOwnerUUID(Player player) {
        Optional<Island> island = Optional.ofNullable(BentoBox.getInstance().getIslands().getIsland(player.getWorld(), player.getUniqueId()));

        if (island.isPresent()) {
            return island.get().getOwner();
        }

        return player.getUniqueId();
    }

    private @NotNull File GetConfiguration(String fileNmae) {
        File File = new File(getDataFolder(), "configurations/" + fileNmae + ".yml");
        if (!File.exists()) saveResource("configurations/" + fileNmae + ".yml", false);

        return File;
    }

    private void loadConfigs() {
        configuration = YamlConfiguration.loadConfiguration(GetConfiguration("config"));
        upgrades = YamlConfiguration.loadConfiguration(GetConfiguration("upgrades"));
        lang = YamlConfiguration.loadConfiguration(GetConfiguration("lang"));
    }

    public static FileConfiguration getConfiguration() { return configuration; }
    public static FileConfiguration getUpgradesConfig() { return upgrades; }
    public static FileConfiguration getLangConfig() { return lang; }

    public static Database getDatabase() { return database; }
    public static SkyCore getInstance() { return instance; }

}