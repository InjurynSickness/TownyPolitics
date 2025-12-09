package com.orbismc.townyPolitics.storage.mysql;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.DatabaseManager;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.storage.AbstractMySQLStorage;
import com.orbismc.townyPolitics.storage.IEntityStorage;
import com.palmergames.bukkit.towny.object.Town;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MySQLTownStorage extends AbstractMySQLStorage implements IEntityStorage<Town> {

    public MySQLTownStorage(TownyPolitics plugin, DatabaseManager dbManager) {
        super(plugin, dbManager, "MySQLTownStorage");
        logger.info("MySQL Town Storage initialized");
    }

    @Override
    public String getEntityType() {
        return "TOWN";
    }

    // ==================== AUTHORITY METHODS ====================
    @Override
    public void saveAuthority(UUID entityUUID, double amount) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO " + prefix + "town_authority (town_uuid, authority_amount) " +
                             "VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE authority_amount = ?")) {

            stmt.setString(1, entityUUID.toString());
            stmt.setDouble(2, amount);
            stmt.setDouble(3, amount);

            stmt.executeUpdate();
            logger.fine("Saved authority for town " + entityUUID + ": " + amount);
        } catch (SQLException e) {
            logger.severe("Failed to save town authority: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, Double> loadAllAuthority() {
        Map<UUID, Double> result = new HashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT town_uuid, authority_amount FROM " + prefix + "town_authority")) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("town_uuid"));
                    double authority = rs.getDouble("authority_amount");
                    result.put(uuid, authority);
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid UUID in database: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to load town authority: " + e.getMessage());
        }

        return result;
    }

    // ==================== DECADENCE METHODS ====================
    @Override
    public void saveDecadence(UUID entityUUID, double amount) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO " + prefix + "town_decadence (town_uuid, decadence_amount) " +
                             "VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE decadence_amount = ?")) {

            stmt.setString(1, entityUUID.toString());
            stmt.setDouble(2, amount);
            stmt.setDouble(3, amount);

            stmt.executeUpdate();
            logger.fine("Saved decadence for town " + entityUUID + ": " + amount);
        } catch (SQLException e) {
            logger.severe("Failed to save town decadence: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, Double> loadAllDecadence() {
        Map<UUID, Double> result = new HashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT town_uuid, decadence_amount FROM " + prefix + "town_decadence")) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("town_uuid"));
                    double decadence = rs.getDouble("decadence_amount");
                    result.put(uuid, decadence);
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid UUID in town decadence database: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to load town decadence: " + e.getMessage());
        }

        return result;
    }

    // ==================== GOVERNMENT METHODS ====================
    @Override
    public void saveGovernment(UUID uuid, GovernmentType type) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO " + prefix + "governments (entity_uuid, entity_type, government_type, last_change_time) " +
                             "VALUES (?, ?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE government_type = ?, last_change_time = ?")) {

            long now = System.currentTimeMillis();

            stmt.setString(1, uuid.toString());
            stmt.setString(2, "TOWN");
            stmt.setString(3, type.name());
            stmt.setLong(4, now);
            stmt.setString(5, type.name());
            stmt.setLong(6, now);

            stmt.executeUpdate();
            logger.fine("Saved government type for town " + uuid + ": " + type.name());
        } catch (SQLException e) {
            logger.severe("Failed to save town government data: " + e.getMessage());
        }
    }

    @Override
    public void saveChangeTime(UUID uuid, long timestamp) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE " + prefix + "governments SET last_change_time = ? " +
                             "WHERE entity_uuid = ? AND entity_type = 'TOWN'")) {

            stmt.setLong(1, timestamp);
            stmt.setString(2, uuid.toString());

            stmt.executeUpdate();
            logger.fine("Updated change time for town " + uuid + " to " + timestamp);
        } catch (SQLException e) {
            logger.severe("Failed to save town government change time: " + e.getMessage());
        }
    }

    @Override
    public GovernmentType getGovernment(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT government_type FROM " + prefix + "governments " +
                             "WHERE entity_uuid = ? AND entity_type = 'TOWN'")) {

            stmt.setString(1, uuid.toString());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String typeName = rs.getString("government_type");
                try {
                    return GovernmentType.valueOf(typeName);
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid government type in database: " + typeName);
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to get town government: " + e.getMessage());
        }

        return GovernmentType.getDefaultTownGovernment(); // Default for towns
    }

    @Override
    public long getChangeTime(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT last_change_time FROM " + prefix + "governments " +
                             "WHERE entity_uuid = ? AND entity_type = 'TOWN'")) {

            stmt.setString(1, uuid.toString());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("last_change_time");
            }
        } catch (SQLException e) {
            logger.severe("Failed to get town change time: " + e.getMessage());
        }

        return 0L; // Default
    }

    @Override
    public Map<UUID, GovernmentType> loadAllGovernments() {
        Map<UUID, GovernmentType> result = new HashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT entity_uuid, government_type FROM " + prefix + "governments " +
                             "WHERE entity_type = 'TOWN'")) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("entity_uuid"));
                    String typeName = rs.getString("government_type");
                    GovernmentType type = GovernmentType.valueOf(typeName);
                    result.put(uuid, type);
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid data in town government database: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to load town governments: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<UUID, Long> loadAllChangeTimes() {
        Map<UUID, Long> result = new HashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT entity_uuid, last_change_time FROM " + prefix + "governments " +
                             "WHERE entity_type = 'TOWN'")) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("entity_uuid"));
                    long time = rs.getLong("last_change_time");
                    result.put(uuid, time);
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid data in town government database: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to load town change times: " + e.getMessage());
        }

        return result;
    }
}