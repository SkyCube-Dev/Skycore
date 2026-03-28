package dev.jackwith.skyCoreV2.databases;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.utils.StackTrace;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class SalesDB implements Database {

    private final SkyCore plugin;
    private Connection connection;

    public SalesDB(SkyCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void connect() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "data/sales.db");

            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
            createTables();

        } catch (Exception e) {
            StackTrace.error("Connecting to sales database", e);
        }
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {

            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS sales (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "uuid TEXT," +
                            "shop_id TEXT," +
                            "item_id TEXT," +
                            "material TEXT," +
                            "amount INTEGER," +
                            "base_price REAL," +
                            "final_price REAL," +
                            "timestamp BIGINT" +
                            ");"
            );

            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS sell_multipliers (" +
                            "uuid TEXT PRIMARY KEY," +
                            "multiplier REAL DEFAULT 1.0" +
                            ");"
            );

        } catch (SQLException e) {
            StackTrace.error("Creating sales tables", e);
        }
    }

    public double getPlayerSellBoost(UUID uuid) {
        String sql = "SELECT multiplier FROM sell_multipliers WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("multiplier");
            }
        } catch (SQLException e) {
            StackTrace.error("Fetching sell boost for " + uuid, e);
        }
        return 1.0;
    }

    public void setPlayerSellBoost(UUID uuid, double boost) {
        String sql = "INSERT INTO sell_multipliers(uuid, multiplier) VALUES(?, ?) " +
                "ON CONFLICT(uuid) DO UPDATE SET multiplier = excluded.multiplier";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setDouble(2, boost);
            ps.executeUpdate();
        } catch (SQLException e) {
            StackTrace.error("Setting sell boost for " + uuid, e);
        }
    }

    public void logSale(UUID uuid, String shopId, String itemId, String material,
                        int amount, double basePrice, double finalPrice) {
        String sql = "INSERT INTO sales(uuid, shop_id, item_id, material, amount, base_price, final_price, timestamp) " +
                "VALUES(?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, shopId);
            ps.setString(3, itemId);
            ps.setString(4, material);
            ps.setInt(5, amount);
            ps.setDouble(6, basePrice);
            ps.setDouble(7, finalPrice);
            ps.setLong(8, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            StackTrace.error("Logging sale for " + uuid, e);
        }
    }

    public Connection getRawConnection() {
        return connection;
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            StackTrace.error("Disconnecting sales database", e);
        }
    }
}