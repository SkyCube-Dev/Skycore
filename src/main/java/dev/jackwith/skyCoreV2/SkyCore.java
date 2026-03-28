package dev.jackwith.skyCoreV2;

import co.aikar.commands.PaperCommandManager;
import dev.jackwith.skyCoreV2.commands.*;
import dev.jackwith.skyCoreV2.databases.*;
import dev.jackwith.skyCoreV2.features.analytics.listeners.LoggerListener;
import dev.jackwith.skyCoreV2.features.playtime.listeners.PlaytimeListener;
import dev.jackwith.skyCoreV2.registeries.CommandRegistry;
import dev.jackwith.skyCoreV2.registeries.DatabaseRegistry;
import dev.jackwith.skyCoreV2.registeries.ListenerRegistry;
import dev.jackwith.skyCoreV2.features.pets.Pet;
import dev.jackwith.skyCoreV2.features.pets.listeners.PetListener;
import dev.jackwith.skyCoreV2.features.pets.PetModel;
import dev.jackwith.skyCoreV2.features.playtime.PlaytimeManager;
import dev.jackwith.skyCoreV2.features.powers.listeners.PowerEffectsListener;
import dev.jackwith.skyCoreV2.features.powers.PowerManager;
import dev.jackwith.skyCoreV2.features.powers.listeners.PowersGUIListener;
import dev.jackwith.skyCoreV2.features.pets.PetService;
import dev.jackwith.skyCoreV2.features.sales.listener.SellListener;
import dev.jackwith.skyCoreV2.features.upgrades.listeners.CropPenaltyListener;
import dev.jackwith.skyCoreV2.features.upgrades.Listeners.IslandCreateListener;
import dev.jackwith.skyCoreV2.features.upgrades.listeners.UpgradeListener;
import dev.jackwith.skyCoreV2.hooks.expansions.BoxExpansion;
import dev.jackwith.skyCoreV2.hooks.expansions.PlaytimeExpansion;
import dev.jackwith.skyCoreV2.utils.StackTrace;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

public final class SkyCore extends JavaPlugin {

    private static SkyCore instance;
    private PlayerPointsAPI ppAPI;
    private Economy econ = null;

    private FileConfiguration petConfig;
    private FileConfiguration langConfig;
    private FileConfiguration powersConfig;
    private FileConfiguration upgradesConfig;

    private PetService petService;
    private PlaytimeManager playtimeManager;
    private PetModel modelManager;

    private PowerDB powerDB;
    private PetsDB petsDB;
    private SalesDB salesDB;
    private AnalyticsDB analyticsDB;

    private UpgradeDB upgradeDatabase;
    private PowerManager powerManager;

    private DatabaseRegistry dbRegistry;
    private CommandRegistry commandRegistry;
    private ListenerRegistry listenerRegistry;

    private PaperCommandManager commandManager;

    public SkyCore() {
        instance = this;
    }

    @Override
    public void onEnable() {
        StackTrace.info("SkyCoreV2 by Jackwith");

        if (Bukkit.getPluginManager().isPluginEnabled("PlayerPoints")) {
            this.ppAPI = PlayerPoints.getInstance().getAPI();
        }

        if (!setupEconomy()) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        StackTrace.info("Loading Plugin...");
        loadDatabases();
        loadConfigs();
        setupPets();

        this.powerManager = new PowerManager();
        this.powerManager.loadPowers();

        this.playtimeManager = new PlaytimeManager(this);
        this.playtimeManager.enable();

        commandRegistry = new CommandRegistry(this);
        commandRegistry.registerAll(
                new PowersCommand(),
                new UpgradesCommand(),
                new SellCommand(),
                new SellBoostCommand(),
                new PetCommand(this.petService)
        );

        commandRegistry.getManager().getCommandCompletions().registerCompletion("players", c ->
                Bukkit.getOnlinePlayers().stream().map(p -> p.getName()).toList()
        );

        commandRegistry.getManager().getCommandCompletions().registerCompletion("pets", c ->
                petService.getAllPets().stream().map(Pet::getId).toList()
        );

        listenerRegistry = new ListenerRegistry(this);
        listenerRegistry.registerAll(
                new PetListener(this.petService),

                new PowersGUIListener(this),
                new PowerEffectsListener(),

                new CropPenaltyListener(this),
                new IslandCreateListener(),
                new UpgradeListener(this),

                new PlaytimeListener(this, playtimeManager.getCache(), analyticsDB),
                new LoggerListener(),

                new SellListener()
        );

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BoxExpansion().register();
            new PlaytimeExpansion(this).register();
        }

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            try {
                modelManager.updatePositions();
            } catch (Exception e) {
                StackTrace.error("PetModel Tick", e);
            }
        }, 0L, 1L);

        new PowerEffectsListener().startTask();
    }

    @Override
    public void onDisable() {
        if (powerDB != null) powerDB.close();
        if (playtimeManager != null) playtimeManager.disable();
        if (petsDB != null) petsDB.close();
    }

    private void loadConfigs() {
        StackTrace.info("(Status) Setting up Configurations");

        File langFile = new File(getDataFolder(), "configurations/lang.yml");
        if (!langFile.exists()) saveResource("configurations/lang.yml", false);
        this.langConfig = YamlConfiguration.loadConfiguration(langFile);

        File petFile = new File(getDataFolder(), "configurations/pets.yml");
        if (!petFile.exists()) saveResource("configurations/pets.yml", false);
        this.petConfig = YamlConfiguration.loadConfiguration(petFile);

        File powersFile = new File(getDataFolder(), "configurations/powers.yml");
        if (!powersFile.exists()) saveResource("configurations/powers.yml", false);
        this.powersConfig = YamlConfiguration.loadConfiguration(powersFile);

        File upgradesFile = new File(getDataFolder(), "configurations/upgrades.yml");
        if (!upgradesFile.exists()) saveResource("configurations/upgrades.yml", false);
        this.upgradesConfig = YamlConfiguration.loadConfiguration(upgradesFile);

        StackTrace.info("(Configurations) Done");
    }

    private void loadDatabases() {
        dbRegistry = new DatabaseRegistry();

        dbRegistry.connectAll(
                new UpgradeDB(),
                new PetsDB(),
                new PowerDB(),
                new AnalyticsDB(),
                new SalesDB(this)
        );

        this.upgradeDatabase = dbRegistry.get(UpgradeDB.class);
        this.petsDB          = dbRegistry.get(PetsDB.class);
        this.powerDB         = dbRegistry.get(PowerDB.class);
        this.salesDB         = dbRegistry.get(SalesDB.class);
        this.analyticsDB     = dbRegistry.get(AnalyticsDB.class);

        StackTrace.info("Databases connected");
    }

    private void setupPets() {
        long start = System.nanoTime();
        StackTrace.info("(Pets) Starting setup...");

        try {
            this.petService = new PetService(petsDB);
            this.modelManager = new PetModel();
            StackTrace.info("(Pets) Services initialized");
            StackTrace.info("(Pets) Listener registered");

            StackTrace.info("(Pets) Commands registered");

            long end = System.nanoTime();
            double ms = (end - start) / 1_000_000.0;
            StackTrace.info("(Pets) Setup complete (" + String.format("%.2f", ms) + "ms)");

        } catch (Exception e) {
            StackTrace.error("(Pets) Setup failed", e);
        }
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
        return econ != null;
    }

    public Economy getEconomy() {
        return econ;
    }

    public void updateIslandSize(String ownerUuid, int newSize, Player actor) {
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
        } catch (Exception e) {
            getLogger().warning("Failed to fire BentoBox IslandEvent: " + e.getMessage());
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

    public void loadPowersConfig() {
        File configFile = new File(this.getDataFolder(), "configurations/powers.yml");
        if (!configFile.exists()) {
            this.saveResource("configurations/powers.yml", false);
        }
        this.powersConfig = YamlConfiguration.loadConfiguration((File)configFile);
    }

    public PlayerPointsAPI getPpAPI() { return this.ppAPI; }

    public FileConfiguration getPetConfig() { return petConfig; }
    public FileConfiguration getPowersConfig() { return powersConfig; }
    public FileConfiguration getLang() { return langConfig; }
    public FileConfiguration getUpgradesConfig() { return upgradesConfig; }

    public PowerDB getPowerDB() { return powerDB; }
    public UpgradeDB getUpgradeDB() { return upgradeDatabase; }
    public SalesDB getsalesDB() { return this.salesDB; }
    public AnalyticsDB getAnalyticsDB() { return this.analyticsDB; }
    public PetsDB getPetsDB() { return this.petsDB; }

    public PetModel getModelManager() { return modelManager; }
    public PetService getPetService() { return petService; }
    public PowerManager getPowerManger() { return this.powerManager; }
    public PlaytimeManager getPlaytimeManager() { return this.playtimeManager; }

    public static SkyCore getInstance() { return instance; }
}