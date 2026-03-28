package dev.jackwith.skyCoreV2.databases;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.databases.data.PlayerPowerData;
import dev.jackwith.skyCoreV2.utils.StackTrace;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PowerDB implements Database {

    private Connection conn;

    private final ConcurrentHashMap<UUID, String> current = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> unlocked = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();

    public void connect() {
        try {
            File file = new File(SkyCore.getInstance().getDataFolder(), "data/powers.db");
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();

            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + file);

            try (Statement st = conn.createStatement()) {
                st.execute("""
                    CREATE TABLE IF NOT EXISTS player_powers (
                        uuid TEXT PRIMARY KEY,
                        current_power TEXT DEFAULT 'none',
                        unlocked_power TEXT DEFAULT 'none'
                    )
                """);
            }

            startAutoFlush();
        } catch (Exception e) {
            StackTrace.error("DB connect", e);
        }
    }

    public void loadUserData(UUID uuid) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT current_power, unlocked_power FROM player_powers WHERE uuid=?")) {

            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                current.put(uuid, rs.getString("current_power"));
                unlocked.put(uuid, rs.getString("unlocked_power"));
            } else {
                current.put(uuid, "none");
                unlocked.put(uuid, "none");
            }

        } catch (SQLException e) {
            StackTrace.error("Load " + uuid, e);
        }
    }

    public String getPower(UUID uuid) {
        return current.getOrDefault(uuid, "none");
    }

    public String getUnlockedPower(UUID uuid) {
        return unlocked.getOrDefault(uuid, "none");
    }

    public boolean hasUsedFreePower(UUID uuid) {
        return !"none".equalsIgnoreCase(getUnlockedPower(uuid));
    }

    public boolean isPowerOwned(UUID uuid, String key) {
        return getUnlockedPower(uuid).equalsIgnoreCase(key);
    }

    public PlayerPowerData getCachedPlayerPower(UUID uuid) {
        String p = current.getOrDefault(uuid, "none");
        String u = unlocked.getOrDefault(uuid, "none");
        return new PlayerPowerData(uuid, p, u, !"none".equalsIgnoreCase(u));
    }

    public void setPower(UUID uuid, String power) {
        current.put(uuid, power);
        dirty.add(uuid);
    }

    public void setPowerOwned(UUID uuid, String power) {
        unlocked.put(uuid, power);
        dirty.add(uuid);
    }

    public void flushDirtyPlayers() {
        if (dirty.isEmpty()) return;

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO player_powers (uuid, current_power, unlocked_power)
                VALUES (?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET current_power=?, unlocked_power=?
            """)) {

                for (UUID id : dirty) {
                    String p = current.getOrDefault(id, "none");
                    String u = unlocked.getOrDefault(id, "none");

                    ps.setString(1, id.toString());
                    ps.setString(2, p);
                    ps.setString(3, u);
                    ps.setString(4, p);
                    ps.setString(5, u);
                    ps.addBatch();
                }

                ps.executeBatch();
                conn.commit();
                dirty.clear();
            }

        } catch (Exception e) {
            StackTrace.error("Flush", e);
        }
    }

    public void unloadUserData(UUID uuid) {
        if (dirty.contains(uuid)) flushDirtyPlayers();
        current.remove(uuid);
        unlocked.remove(uuid);
    }

    public void close() {
        flushDirtyPlayers();
        try {
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (Exception e) {
            StackTrace.error("Close DB", e);
        }
    }

    private void startAutoFlush() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                SkyCore.getInstance(),
                this::flushDirtyPlayers,
                200L,
                200L
        );
    }
}