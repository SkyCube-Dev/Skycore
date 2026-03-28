package dev.jackwith.skyCoreV2.databases;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.databases.data.PlayerPowerData;
import dev.jackwith.skyCoreV2.utils.StackTrace;
import java.io.File;
import java.sql.*;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class PowerDB implements Database {

    private Connection connection;
    private final ConcurrentHashMap<UUID, String> currentPowerCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> unlockedPowerCache = new ConcurrentHashMap<>();
    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();

    /** Connect to SQLite database and create table if it doesn't exist */
    public void connect() {
        try {
            File dbFile = new File(SkyCore.getInstance().getDataFolder(), "data/powers.db");
            if (!dbFile.getParentFile().exists()) dbFile.getParentFile().mkdirs();

            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);

            try (Statement statement = this.connection.createStatement()) {
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS player_powers (
                        uuid TEXT PRIMARY KEY,
                        current_power TEXT DEFAULT 'none',
                        unlocked_power TEXT DEFAULT 'none'
                    )
                    """);
            }

            startAutoFlushTask();
        } catch (Exception e) {
            StackTrace.error("Database connect", e);
        }
    }

    public void loadUserData(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT current_power, unlocked_power FROM player_powers WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                currentPowerCache.put(uuid, rs.getString("current_power"));
                unlockedPowerCache.put(uuid, rs.getString("unlocked_power"));
            } else {
                currentPowerCache.put(uuid, "none");
                unlockedPowerCache.put(uuid, "none");
            }
        } catch (SQLException e) {
            StackTrace.error("Load user data for " + uuid, e);
        }
    }

    /** Get the player's currently active power */
    public String getPower(UUID uuid) {
        return currentPowerCache.getOrDefault(uuid, "none");
    }

    /** Get the player's unlocked power */
    public String getUnlockedPower(UUID uuid) {
        return unlockedPowerCache.getOrDefault(uuid, "none");
    }

    /** Check if player has used a free power (unlocked something) */
    public boolean hasUsedFreePower(UUID uuid) {
        return !getUnlockedPower(uuid).equalsIgnoreCase("none");
    }

    /** Check if a player owns a specific power */
    public boolean isPowerOwned(UUID uuid, String powerKey) {
        return getUnlockedPower(uuid).equalsIgnoreCase(powerKey);
    }

    /** Get full cached player power data */
    public PlayerPowerData getCachedPlayerPower(UUID uuid) {
        String power = currentPowerCache.getOrDefault(uuid, "none");
        String unlocked = unlockedPowerCache.getOrDefault(uuid, "none");
        boolean usedFree = !unlocked.equalsIgnoreCase("none");
        return new PlayerPowerData(uuid, power, unlocked, usedFree);
    }

    /** Set the player's current power and mark dirty for flush */
    public void setPower(UUID uuid, String powerKey) {
        currentPowerCache.put(uuid, powerKey);
        dirtyPlayers.add(uuid);
    }

    /** Unlock a power for the player and mark dirty */
    public void setPowerOwned(UUID uuid, String powerKey) {
        unlockedPowerCache.put(uuid, powerKey);
        dirtyPlayers.add(uuid);
    }

    /** Flush all dirty players to database */
    public void flushDirtyPlayers() {
        if (dirtyPlayers.isEmpty()) return;

        try {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO player_powers (uuid, current_power, unlocked_power)
                    VALUES (?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET current_power=?, unlocked_power=?
                    """)) {

                for (UUID uuid : dirtyPlayers) {
                    String current = currentPowerCache.getOrDefault(uuid, "none");
                    String unlocked = unlockedPowerCache.getOrDefault(uuid, "none");

                    ps.setString(1, uuid.toString());
                    ps.setString(2, current);
                    ps.setString(3, unlocked);
                    ps.setString(4, current);
                    ps.setString(5, unlocked);
                    ps.addBatch();
                }

                ps.executeBatch();
                connection.commit();
                dirtyPlayers.clear();
            }
        } catch (Exception e) {
            StackTrace.error("Flush dirty players", e);
        }
    }

    /** Unload a player from cache (flush if dirty) */
    public void unloadUserData(UUID uuid) {
        if (dirtyPlayers.contains(uuid)) flushDirtyPlayers();
        currentPowerCache.remove(uuid);
        unlockedPowerCache.remove(uuid);
    }

    /** Close the database safely */
    public void close() {
        flushDirtyPlayers();
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (Exception e) {
            StackTrace.error("Close database", e);
        }
    }

    public Connection getRawConnection() {
        return connection;
    }

    /** Automatically flush dirty players every 10s (200 ticks) */
    private void startAutoFlushTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                (Plugin) SkyCore.getInstance(),
                this::flushDirtyPlayers,
                200L, // initial delay
                200L  // repeat interval
        );
    }
}