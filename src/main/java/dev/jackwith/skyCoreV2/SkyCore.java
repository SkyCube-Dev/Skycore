package dev.jackwith.skyCoreV2;

import dev.jackwith.skyCoreV2.commands.PetCommand;
import dev.jackwith.skyCoreV2.commands.UpgradesCommand;
import dev.jackwith.skyCoreV2.database.Database;
import dev.jackwith.skyCoreV2.database.PetsCollection;
import dev.jackwith.skyCoreV2.database.UpgradesCollection;
import dev.jackwith.skyCoreV2.features.pets.Pet;
import dev.jackwith.skyCoreV2.features.pets.PetModel;
import dev.jackwith.skyCoreV2.features.pets.PetService;
import dev.jackwith.skyCoreV2.features.pets.listeners.PetListener;
import dev.jackwith.skyCoreV2.features.upgrades.listeners.CropListener;
import dev.jackwith.skyCoreV2.features.upgrades.listeners.IslandCreation;
import dev.jackwith.skyCoreV2.features.upgrades.listeners.UpgradeListener;
import dev.jackwith.skyCoreV2.registeries.CommandRegistry;
import dev.jackwith.skyCoreV2.utils.StackTrace;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
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
    private static FileConfiguration pets;
    private static FileConfiguration powers;

    private static PetModel modelManager;
    private static PetService petService;

    private static Database database;
    private static UpgradesCollection UpgradesCollection;
    private static PetsCollection petsCollection;

    public SkyCore() {
        instance = this;
    }

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlayerPoints")) {
            this.ppAPI = PlayerPoints.getInstance().getAPI();
        }

        if (!setupEconomy()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        loadConfigs();

        database = new Database();
        UpgradesCollection = new UpgradesCollection();
        petsCollection = new PetsCollection();

        petService = new PetService(petsCollection);
        modelManager = new PetModel();

        CommandRegistry commandRegistry = new CommandRegistry(this);
        commandRegistry.registerAll(
                new UpgradesCommand(),
                new PetCommand(petService)
        );

        commandRegistry.manager().getCommandCompletions().registerCompletion("players", c ->
                Bukkit.getOnlinePlayers().stream().map(Player::getName).toList()
        );

        commandRegistry.manager().getCommandCompletions().registerCompletion("pets", c ->
                petService.getAllPets().stream().map(Pet::id).toList()
        );



        Bukkit.getScheduler().runTaskTimer(this, () -> {
            try {
                modelManager.updatePositions();
            } catch (Exception e) {
                StackTrace.error("PetModel Tick", e);
            }
        }, 0L, 1L);

        getServer().getPluginManager().registerEvents(new PetListener(petService), this);

        getServer().getPluginManager().registerEvents(new IslandCreation(), this);
        getServer().getPluginManager().registerEvents(new CropListener(), this);
        getServer().getPluginManager().registerEvents(new UpgradeListener(this), this);



    }

    @Override
    public void onDisable() {
        database.disconnect();
    }

    private @NotNull File GetConfiguration(String fileName) {
        File File = new File(getDataFolder(), "configurations/" + fileName + ".yml");
        if (!File.exists()) saveResource("configurations/" + fileName + ".yml", false);

        return File;
    }

    private void loadConfigs() {
        configuration = YamlConfiguration.loadConfiguration(GetConfiguration("config"));
        upgrades = YamlConfiguration.loadConfiguration(GetConfiguration("upgrades"));
        lang = YamlConfiguration.loadConfiguration(GetConfiguration("lang"));
        pets = YamlConfiguration.loadConfiguration(GetConfiguration("pets"));
        powers = YamlConfiguration.loadConfiguration(GetConfiguration("powers"));
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
    public static FileConfiguration getPetsConfig() { return pets; }
    public static FileConfiguration getPowersConfig() { return powers; }

    public static Database getDatabase() { return database; }
    public static UpgradesCollection getUpgradesCollection() { return UpgradesCollection; }
    public static PetsCollection getPetsCollection() { return petsCollection; }

    public static PetService getPetService() { return petService; }
    public static PetModel getModelManager() { return modelManager; }

    public static SkyCore getInstance() { return instance; }

}