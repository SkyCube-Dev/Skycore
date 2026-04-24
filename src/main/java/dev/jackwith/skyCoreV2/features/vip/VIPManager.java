package dev.jackwith.skyCoreV2.features.vip;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.jackwith.skyCoreV2.SkyCore;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

public class VIPManager {

    private final SkyCore plugin;
    private final File dataFile;
    private final Gson gson;

    private VIPDatabase database;
    private Map<String, VIPSubscription> activeSubscriptions;

    public VIPManager(SkyCore plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.activeSubscriptions = new HashMap<>();
    }

    /**
     * Load VIP database from data.json
     */
    public boolean loadDatabase() {
        if (!dataFile.exists()) {
            plugin.getLogger().warning("VIP database file not found: " + dataFile.getPath());
            plugin.getLogger().warning("Please run the scanner and place data.json in the plugin folder");
            return false;
        }

        try (FileReader reader = new FileReader(dataFile)) {
            database = gson.fromJson(reader, VIPDatabase.class);

            if (database == null) {
                plugin.getLogger().severe("Failed to parse VIP database - file may be corrupted");
                return false;
            }

            // Build active subscriptions map for fast lookup
            activeSubscriptions.clear();
            if (database.getActiveUsers() != null) {
                for (VIPSubscription sub : database.getActiveUsers()) {
                    activeSubscriptions.put(sub.getUsername().toLowerCase(), sub);
                }
            }

            plugin.getLogger().info("VIP database loaded successfully");
            plugin.getLogger().info("Active subscriptions: " + activeSubscriptions.size());
            plugin.getLogger().info("Total users: " + (database.getUsers() != null ? database.getUsers().size() : 0));

            return true;

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load VIP database", e);
            return false;
        }
    }

    /**
     * Reload database from disk
     */
    public boolean reloadDatabase() {
        plugin.getLogger().info("Reloading VIP database...");
        return loadDatabase();
    }

    /**
     * Check if a player has an active VIP subscription
     */
    public boolean hasActiveSubscription(String username) {
        return activeSubscriptions.containsKey(username.toLowerCase());
    }

    /**
     * Get active subscription for a player
     */
    public Optional<VIPSubscription> getSubscription(String username) {
        VIPSubscription sub = activeSubscriptions.get(username.toLowerCase());
        return Optional.ofNullable(sub);
    }

    /**
     * Grant VIP to player using LuckPerms command
     */
    public void grantVIP(String username, VIPSubscription subscription) {
        String rank = subscription.getRank();
        String duration = subscription.getFormattedDuration();

        // Build LuckPerms command: lp user <username> parent add <rank> <duration>
        String command = String.format("lp user %s parent add %s %s",
                username, rank, duration);

        // Execute command as console
        Bukkit.getScheduler().runTask(plugin, () -> {
            boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

            if (success) {
                plugin.getLogger().info(String.format(
                        "Granted %s to %s for %s (%d days)",
                        rank.toUpperCase(), username, duration, subscription.getDaysRemaining()
                ));
            } else {
                plugin.getLogger().warning(String.format(
                        "Failed to grant %s to %s - check LuckPerms installation",
                        rank.toUpperCase(), username
                ));
            }
        });
    }

    /**
     * Process player join - grant VIP if they have active subscription
     */
    public void processPlayerJoin(String username) {
        Optional<VIPSubscription> subscription = getSubscription(username);

        if (subscription.isPresent()) {
            VIPSubscription sub = subscription.get();

            // Only grant if they have time remaining
            if (sub.getDaysRemaining() > 0) {
                plugin.getLogger().info(String.format(
                        "Player %s has active %s subscription (%d days remaining)",
                        username, sub.getRank().toUpperCase(), sub.getDaysRemaining()
                ));

                grantVIP(username, sub);
            } else {
                plugin.getLogger().warning(String.format(
                        "Player %s subscription expired - 0 days remaining",
                        username
                ));
            }
        }
    }

    /**
     * Get database instance
     */
    public VIPDatabase getDatabase() {
        return database;
    }

    /**
     * Get total active subscriptions
     */
    public int getActiveCount() {
        return activeSubscriptions.size();
    }

    /**
     * Check if database is loaded
     */
    public boolean isDatabaseLoaded() {
        return database != null && !activeSubscriptions.isEmpty();
    }
}