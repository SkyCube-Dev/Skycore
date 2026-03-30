package dev.jackwith.skyCoreV2;

import dev.jackwith.skyCoreV2.commands.UpgradesCommand;
import dev.jackwith.skyCoreV2.database.Database;
import dev.jackwith.skyCoreV2.database.UpgradesCollection;
import dev.jackwith.skyCoreV2.features.upgrades.listeners.CropListener;
import dev.jackwith.skyCoreV2.features.upgrades.listeners.IslandCreation;
import dev.jackwith.skyCoreV2.features.upgrades.listeners.UpgradeListener;
import dev.jackwith.skyCoreV2.registeries.CommandRegistry;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public final class SkyCore extends JavaPlugin {

    private static SkyCore instance;
    private PlayerPointsAPI ppAPI;
    private Economy econ = null;

    private static FileConfiguration configuration;
    private static FileConfiguration upgrades;
    private static FileConfiguration lang;

    private static Database database;
    private static UpgradesCollection UpgradesCollection;

    public SkyCore() {
        instance = this;
    }

    @Override
    public void onEnable() {
        loadConfigs();

        database = new Database();
        UpgradesCollection = new UpgradesCollection();

        if (Bukkit.getPluginManager().isPluginEnabled("PlayerPoints")) {
            this.ppAPI = PlayerPoints.getInstance().getAPI();
        }

        if (!setupEconomy()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        CommandRegistry commandRegistry = new CommandRegistry(this);
        commandRegistry.registerAll(
                new UpgradesCommand()
        );

        getServer().getPluginManager().registerEvents(new IslandCreation(), this);
        getServer().getPluginManager().registerEvents(new CropListener(this), this);
        getServer().getPluginManager().registerEvents(new UpgradeListener(this), this);

    }

    @Override
    public void onDisable() {
        database.disconnect();
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

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return true;
    }

    public Economy getEconomy() { return econ; }

    public PlayerPointsAPI getPpAPI() { return this.ppAPI; }

    public static FileConfiguration getConfiguration() { return configuration; }
    public static FileConfiguration getUpgradesConfig() { return upgrades; }
    public static FileConfiguration getLangConfig() { return lang; }

    public static Database getDatabase() { return database; }
    public static UpgradesCollection getUpgradesCollection() { return UpgradesCollection; }

    public static SkyCore getInstance() { return instance; }

}