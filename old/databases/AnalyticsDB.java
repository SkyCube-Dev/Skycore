package dev.jackwith.skyCoreV2.databases;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.utils.StackTrace;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnalyticsDB implements Database {

    private Connection connection;
    private final ExecutorService exec = Executors.newSingleThreadExecutor();

    @Override
    public void connect() {
        try {
            File file = new File(SkyCore.getInstance().getDataFolder(), "data/analytics.db");
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + file.getAbsolutePath() + "?journal_mode=WAL");
            createTables();
        } catch (Exception e) {
            StackTrace.error("AnalyticsDB connect", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS player_totals (
                    uuid TEXT PRIMARY KEY,
                    total_onlinetime INTEGER DEFAULT 0,
                    total_afktime INTEGER DEFAULT 0,
                    last_joined TEXT,
                    total_joins INTEGER DEFAULT 0
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS player_joins (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT,
                    username TEXT,
                    ip TEXT,
                    join_date TEXT,
                    joined_from TEXT
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS player_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT,
                    type TEXT,
                    content TEXT,
                    timestamp TEXT
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS player_daily_stats (
                    uuid TEXT,
                    log_date TEXT,
                    daily_online INTEGER DEFAULT 0,
                    daily_afk INTEGER DEFAULT 0,
                    PRIMARY KEY(uuid, log_date)
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS player_hourly_stats (
                    uuid TEXT,
                    log_hour TEXT,
                    minutes INTEGER DEFAULT 0,
                    PRIMARY KEY(uuid, log_hour)
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS player_5min_status (
                    uuid TEXT,
                    log_slot TEXT,
                    status TEXT,
                    PRIMARY KEY(uuid, log_slot)
                )
            """);

            st.execute("CREATE INDEX IF NOT EXISTS idx_hour_uuid ON player_hourly_stats(uuid)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_status_slot ON player_5min_status(log_slot)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_status_uuid ON player_5min_status(uuid)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_status_uuid_slot ON player_5min_status(uuid, log_slot)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_joins_uuid ON player_joins(uuid)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_joins_ip ON player_joins(ip)");
        }
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) connect();
        return connection;
    }

    public boolean checkJoin(String uuid, String username, String ip, String joinedFrom) {
        String now = LocalDateTime.now().toString();
        boolean newPlayer = false;

        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT uuid FROM player_totals WHERE uuid = ?")) {
                ps.setString(1, uuid);
                newPlayer = !ps.executeQuery().next();
            }

            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO player_totals(uuid, last_joined, total_joins)
                    VALUES(?, ?, 1)
                    ON CONFLICT(uuid)
                    DO UPDATE SET last_joined = ?, total_joins = total_joins + 1
                    """)) {
                ps.setString(1, uuid);
                ps.setString(2, now);
                ps.setString(3, now);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO player_joins(uuid, username, ip, join_date, joined_from) VALUES(?,?,?,?,?)")) {
                ps.setString(1, uuid);
                ps.setString(2, username);
                ps.setString(3, ip);
                ps.setString(4, now);
                ps.setString(5, joinedFrom);
                ps.executeUpdate();
            }

        } catch (Exception e) {
            StackTrace.error("AnalyticsDB:checkJoin" + uuid, e);
        }

        return newPlayer;
    }

    public void addPlaytime(String uuid, long seconds, boolean isAfk) {
        exec.submit(() -> {
            String fulltime = isAfk ? "afktime"   : "onlinetime";
            String dailytime = isAfk ? "daily_afk" : "daily_online";
            String today = LocalDate.now().toString();
            LocalDateTime now = LocalDateTime.now();
            String hour = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH"));
            int minute = now.getMinute() / 5 * 5;
            String slot = now.withMinute(minute).withSecond(0).withNano(0)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            try (Connection conn = getConnection()) {

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO player_totals(uuid, total_" + fulltime + ") VALUES(?,?) " +
                                "ON CONFLICT(uuid) DO UPDATE SET total_" + fulltime + " = total_" + fulltime + " + ?")) {
                    ps.setString(1, uuid);
                    ps.setLong(2, seconds);
                    ps.setLong(3, seconds);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO player_daily_stats(uuid, log_date, " + dailytime + ") VALUES(?,?,?) " +
                                "ON CONFLICT(uuid, log_date) DO UPDATE SET " + dailytime + " = " + dailytime + " + ?")) {
                    ps.setString(1, uuid);
                    ps.setString(2, today);
                    ps.setLong(3, seconds);
                    ps.setLong(4, seconds);
                    ps.executeUpdate();
                }

                long mynutaties = Math.max(1L, seconds / 60L);

                if (!isAfk) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO player_hourly_stats(uuid, log_hour, minutes) VALUES(?,?,?) " +
                                    "ON CONFLICT(uuid, log_hour) DO UPDATE SET minutes = minutes + ?")) {
                        ps.setString(1, uuid);
                        ps.setString(2, hour);
                        ps.setLong(3, mynutaties);
                        ps.setLong(4, mynutaties);
                        ps.executeUpdate();
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR REPLACE INTO player_5min_status(uuid, log_slot, status) VALUES(?,?,?)")) {
                    ps.setString(1, uuid);
                    ps.setString(2, slot);
                    ps.setString(3, isAfk ? "AFK" : "ONLINE");
                    ps.executeUpdate();
                }

            } catch (Exception e) {
                StackTrace.error("Playtime" , e);
            }
        });
    }

    private String formatSeconds(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        return hours + "h " + minutes + "m";
    }

    private String uuidUser(String uuid) {
        try {
            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
            if (player.getName() != null) {
                return player.getName();
            }
        } catch (Exception ignored) {}
        return uuid;
    }

    public List<String[]> topPlaytime(int limit) {
        List<String[]> top = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                 SELECT uuid, total_onlinetime + total_afktime AS total_time
                 FROM player_totals
                 ORDER BY total_time DESC
                 LIMIT ?
                 """)) {

            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String uuid = rs.getString("uuid");
                long totalTimeSeconds = rs.getLong("total_time");

                String username = uuidUser(uuid);
                String formattedTime = formatSeconds(totalTimeSeconds);

                top.add(new String[]{username, formattedTime});
            }

        } catch (Exception e) {
            StackTrace.error("AnalyticsDB:TopPlaytime", e);
        }

        return top;
    }

    public int totalPlayerCount() {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM player_totals")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            StackTrace.error("AnalyticsDB:PlayerCount", e);
        }
        return 0;
    }

    public void logAction(String uuid, String type, String content) {
        Bukkit.getScheduler().runTaskAsynchronously(SkyCore.getInstance(), () -> {
            String timestamp = LocalDateTime.now().toString();
            try (PreparedStatement ps = getConnection().prepareStatement(
                    "INSERT INTO player_logs(uuid, type, content, timestamp) VALUES(?,?,?,?)")) {
                ps.setString(1, uuid);
                ps.setString(2, type);
                ps.setString(3, content);
                ps.setString(4, timestamp);
                ps.executeUpdate();
            } catch (Exception e) {
                StackTrace.error("AnalyticsDB:logAction" + uuid, e);
            }
        });
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            StackTrace.error("AnalyticsDB close", e);
        }
    }

    public record PlaytimeData(long totalOnline, long totalAfk) {
        public long getCombinedTime() {
            return totalOnline + totalAfk;
        }
    }
}