package dev.jackwith.skyCoreV2.databases;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.utils.StackTrace;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UpgradeDB implements Database {

    private Connection connection;

    private final ConcurrentHashMap<UUID, Integer> levelCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> upgradingUntilCache = new ConcurrentHashMap<>();

    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();

    public void connect() {
        try {
            File dbFile = new File(SkyCore.getInstance().getDataFolder(), "data/upgrades.db");
            if (!dbFile.getParentFile().exists()) dbFile.getParentFile().mkdirs();

            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);

            try (Statement statement = this.connection.createStatement()) {
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS island_upgrades (
                        uuid TEXT PRIMARY KEY,
                        level INTEGER DEFAULT 0,
                        upgrading_until BIGINT DEFAULT 0
                    )
                    """);
            }

            startAutoFlushTask();

        } catch (Exception e) {
            StackTrace.error("UpgradeDB connect", e);
        }
    }

    public void loadUserData(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT level, upgrading_until FROM island_upgrades WHERE uuid=?")) {

            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                levelCache.put(uuid, rs.getInt("level"));
                upgradingUntilCache.put(uuid, rs.getLong("upgrading_until"));
            } else {
                levelCache.put(uuid, 0);
                upgradingUntilCache.put(uuid, 0L);
            }

        } catch (SQLException e) {
            StackTrace.error("Load upgrade data for " + uuid, e);
        }
    }

    public int getLevel(UUID uuid) {
        return levelCache.getOrDefault(uuid, 1);
    }

    public long getUpgradingUntil(UUID uuid) {
        return upgradingUntilCache.getOrDefault(uuid, 0L);
    }

    public void setLevel(UUID uuid, int level) {
        levelCache.put(uuid, level);
        dirtyPlayers.add(uuid);
    }

    public void setUpgradingUntil(UUID uuid, long time) {
        upgradingUntilCache.put(uuid, time);
        dirtyPlayers.add(uuid);
    }

    public boolean isUpgrading(UUID uuid) {
        return getUpgradingUntil(uuid) > System.currentTimeMillis();
    }

    public void clearExpired(UUID uuid) {
        upgradingUntilCache.put(uuid, 0L);
        dirtyPlayers.add(uuid);
    }


    public void flushDirtyPlayers() {
        if (dirtyPlayers.isEmpty()) return;

        try {
            connection.setAutoCommit(false);

            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO island_upgrades (uuid, level, upgrading_until)
                    VALUES (?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET level=?, upgrading_until=?
                    """)) {

                for (UUID uuid : dirtyPlayers) {
                    int level = levelCache.getOrDefault(uuid, 0);
                    long upgrading = upgradingUntilCache.getOrDefault(uuid, 0L);

                    ps.setString(1, uuid.toString());
                    ps.setInt(2, level);
                    ps.setLong(3, upgrading);
                    ps.setInt(4, level);
                    ps.setLong(5, upgrading);
                    ps.addBatch();
                }

                ps.executeBatch();
                connection.commit();
                dirtyPlayers.clear();
            }

        } catch (Exception e) {
            StackTrace.error("Flush upgrade players", e);
        }
    }

    public void unloadUserData(UUID uuid) {
        if (dirtyPlayers.contains(uuid)) flushDirtyPlayers();
        levelCache.remove(uuid);
        upgradingUntilCache.remove(uuid);
    }

    public void close() {
        flushDirtyPlayers();
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (Exception e) {
            StackTrace.error("Close UpgradeDB", e);
        }
    }

    private void startAutoFlushTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                SkyCore.getInstance(),
                this::flushDirtyPlayers,
                200L,
                200L
        );
    }
}