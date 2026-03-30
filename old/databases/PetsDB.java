package dev.jackwith.skyCoreV2.databases;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.utils.StackTrace;

import java.io.File;
import java.sql.*;
import java.util.*;

public class PetsDB implements Database {

    private static final String TABLE_NAME = "player_pets";

    private static final String SELECT_ALL_PETS =
            "SELECT pet_id FROM " + TABLE_NAME + " WHERE uuid = ?";

    private static final String SELECT_EQUIPPED_PETS =
            "SELECT pet_id FROM " + TABLE_NAME + " WHERE uuid = ? AND is_equipped = 1";

    private static final String SELECT_ACQUIRED =
            "SELECT pet_id, acquired_at FROM " + TABLE_NAME + " WHERE uuid = ?";

    private static final String INSERT_PET =
            "INSERT OR IGNORE INTO " + TABLE_NAME + " (uuid, pet_id) VALUES (?, ?)";

    private static final String DELETE_PET =
            "DELETE FROM " + TABLE_NAME + " WHERE uuid = ? AND pet_id = ?";

    private static final String UPDATE_EQUIPPED =
            "UPDATE " + TABLE_NAME + " SET is_equipped = ? WHERE uuid = ? AND pet_id = ?";

    private static final String UNEQUIP_ALL =
            "UPDATE " + TABLE_NAME + " SET is_equipped = 0 WHERE uuid = ?";

    private Connection connection;

    public void connect() {
        try {
            File dbFile = new File(SkyCore.getInstance().getDataFolder(), "data/pets.db");

            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);

            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_pets (
                        uuid TEXT,
                        pet_id TEXT,
                        is_equipped INTEGER DEFAULT 0,
                        acquired_at INTEGER DEFAULT 0,
                        PRIMARY KEY (uuid, pet_id)
                    )
                """);
            }

        } catch (Exception e) {
            StackTrace.error("Connecting to pets database", e);
        }
    }

    private String uuidToString(UUID uuid) {
        return uuid.toString();
    }

    public Map<String, Long> getAllPetAcquisitionDates(UUID uuid) {
        Map<String, Long> map = new HashMap<>();

        try (PreparedStatement ps = connection.prepareStatement(SELECT_ACQUIRED)) {
            ps.setString(1, uuidToString(uuid));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("pet_id"), rs.getLong("acquired_at"));
                }
            }

        } catch (SQLException e) {
            StackTrace.error("Fetching acquisition dates for " + uuid, e);
        }

        return map;
    }

    public void addPet(UUID uuid, String petId) {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_PET)) {
            ps.setString(1, uuidToString(uuid));
            ps.setString(2, petId);
            ps.executeUpdate();

        } catch (SQLException e) {
            StackTrace.error("Adding pet '" + petId + "' to " + uuid, e);
        }
    }

    public void deletePet(UUID uuid, String petId) {
        try (PreparedStatement ps = connection.prepareStatement(DELETE_PET)) {
            ps.setString(1, uuidToString(uuid));
            ps.setString(2, petId);
            ps.executeUpdate();

        } catch (SQLException e) {
            StackTrace.error("Deleting pet '" + petId + "' from " + uuid, e);
        }
    }

    public List<String> getOwnedPets(UUID uuid) {
        return getPetList(uuid, SELECT_ALL_PETS);
    }

    public List<String> getEquippedPets(UUID uuid) {
        return getPetList(uuid, SELECT_EQUIPPED_PETS);
    }

    private List<String> getPetList(UUID uuid, String query) {
        List<String> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, uuidToString(uuid));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("pet_id"));
                }
            }

        } catch (SQLException e) {
            StackTrace.error("Fetching pet list for " + uuid, e);
        }

        return list;
    }

    public void setEquipped(UUID uuid, String petId, boolean equipped) {
        try (PreparedStatement ps = connection.prepareStatement(UPDATE_EQUIPPED)) {
            ps.setInt(1, equipped ? 1 : 0);
            ps.setString(2, uuidToString(uuid));
            ps.setString(3, petId);
            ps.executeUpdate();

        } catch (SQLException e) {
            StackTrace.error("Setting equipped state for pet '" + petId + "' (" + equipped + ")", e);
        }
    }

    public void unequipAll(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(UNEQUIP_ALL)) {
            ps.setString(1, uuidToString(uuid));
            ps.executeUpdate();

        } catch (SQLException e) {
            StackTrace.error("Unequipping all pets for " + uuid, e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            StackTrace.error("Disconnecting pets database", e);
        }
    }
}