// MySQLVassalageStorage.java
package com.orbismc.townyPolitics.storage.mysql;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.DatabaseManager;
import com.orbismc.townyPolitics.storage.AbstractMySQLStorage;
import com.orbismc.townyPolitics.storage.IVassalageStorage;
import com.orbismc.townyPolitics.vassalage.VassalageRelationship;
import com.orbismc.townyPolitics.vassalage.VassalageOffer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MySQLVassalageStorage extends AbstractMySQLStorage implements IVassalageStorage {

    public MySQLVassalageStorage(TownyPolitics plugin, DatabaseManager dbManager) {
        super(plugin, dbManager, "MySQLVassalageStorage");
        logger.info("MySQL Vassalage Storage initialized");
        createTables();
    }

    private void createTables() {
        try (Connection conn = getConnection()) {
            // Vassalage relationships table
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + prefix + "vassalage_relationships (" +
                            "liege_uuid VARCHAR(36) NOT NULL, " +
                            "vassal_uuid VARCHAR(36) NOT NULL, " +
                            "tribute_rate DOUBLE NOT NULL, " +
                            "established_time BIGINT NOT NULL, " +
                            "last_tribute_time BIGINT NOT NULL, " +
                            "PRIMARY KEY (liege_uuid, vassal_uuid), " +
                            "INDEX idx_liege (liege_uuid), " +
                            "INDEX idx_vassal (vassal_uuid)" +
                            ") ENGINE=InnoDB")) {
                stmt.executeUpdate();
                logger.fine("Created/verified vassalage_relationships table");
            }

            // Vassalage offers table
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + prefix + "vassalage_offers (" +
                            "offer_uuid VARCHAR(36) PRIMARY KEY, " +
                            "liege_uuid VARCHAR(36) NOT NULL, " +
                            "target_uuid VARCHAR(36) NOT NULL, " +
                            "proposed_tribute_rate DOUBLE NOT NULL, " +
                            "offer_time BIGINT NOT NULL, " +
                            "expiry_time BIGINT NOT NULL, " +
                            "INDEX idx_liege_offers (liege_uuid), " +
                            "INDEX idx_target_offers (target_uuid), " +
                            "INDEX idx_expiry (expiry_time)" +
                            ") ENGINE=InnoDB")) {
                stmt.executeUpdate();
                logger.fine("Created/verified vassalage_offers table");
            }

            logger.info("Successfully created/verified vassalage database tables");
        } catch (SQLException e) {
            logger.severe("Failed to create vassalage database tables: " + e.getMessage());
        }
    }

    // Relationship methods
    @Override
    public void saveRelationship(VassalageRelationship relationship) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO " + prefix + "vassalage_relationships " +
                             "(liege_uuid, vassal_uuid, tribute_rate, established_time, last_tribute_time) " +
                             "VALUES (?, ?, ?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE " +
                             "tribute_rate = ?, last_tribute_time = ?")) {

            stmt.setString(1, relationship.getLiegeUUID().toString());
            stmt.setString(2, relationship.getVassalUUID().toString());
            stmt.setDouble(3, relationship.getTributeRate());
            stmt.setLong(4, relationship.getEstablishedTime());
            stmt.setLong(5, relationship.getLastTributeTime());
            stmt.setDouble(6, relationship.getTributeRate());
            stmt.setLong(7, relationship.getLastTributeTime());

            stmt.executeUpdate();
            logger.fine("Saved vassalage relationship: " + relationship);
        } catch (SQLException e) {
            logger.severe("Failed to save vassalage relationship: " + e.getMessage());
        }
    }

    @Override
    public void removeRelationship(UUID liegeUUID, UUID vassalUUID) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM " + prefix + "vassalage_relationships " +
                             "WHERE liege_uuid = ? AND vassal_uuid = ?")) {

            stmt.setString(1, liegeUUID.toString());
            stmt.setString(2, vassalUUID.toString());

            int deleted = stmt.executeUpdate();
            logger.fine("Removed vassalage relationship: liege=" + liegeUUID + 
                       ", vassal=" + vassalUUID + " (rows: " + deleted + ")");
        } catch (SQLException e) {
            logger.severe("Failed to remove vassalage relationship: " + e.getMessage());
        }
    }

    @Override
    public Optional<VassalageRelationship> getRelationship(UUID liegeUUID, UUID vassalUUID) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM " + prefix + "vassalage_relationships " +
                             "WHERE liege_uuid = ? AND vassal_uuid = ?")) {

            stmt.setString(1, liegeUUID.toString());
            stmt.setString(2, vassalUUID.toString());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(new VassalageRelationship(
                        UUID.fromString(rs.getString("liege_uuid")),
                        UUID.fromString(rs.getString("vassal_uuid")),
                        rs.getDouble("tribute_rate"),
                        rs.getLong("established_time"),
                        rs.getLong("last_tribute_time")
                ));
            }
        } catch (SQLException e) {
            logger.severe("Failed to get vassalage relationship: " + e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public List<VassalageRelationship> getVassalsOf(UUID liegeUUID) {
        List<VassalageRelationship> vassals = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM " + prefix + "vassalage_relationships WHERE liege_uuid = ?")) {

            stmt.setString(1, liegeUUID.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                vassals.add(new VassalageRelationship(
                        UUID.fromString(rs.getString("liege_uuid")),
                        UUID.fromString(rs.getString("vassal_uuid")),
                        rs.getDouble("tribute_rate"),
                        rs.getLong("established_time"),
                        rs.getLong("last_tribute_time")
                ));
            }
        } catch (SQLException e) {
            logger.severe("Failed to get vassals: " + e.getMessage());
        }

        return vassals;
    }

    @Override
    public Optional<VassalageRelationship> getLiegeOf(UUID vassalUUID) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM " + prefix + "vassalage_relationships WHERE vassal_uuid = ?")) {

            stmt.setString(1, vassalUUID.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(new VassalageRelationship(
                        UUID.fromString(rs.getString("liege_uuid")),
                        UUID.fromString(rs.getString("vassal_uuid")),
                        rs.getDouble("tribute_rate"),
                        rs.getLong("established_time"),
                        rs.getLong("last_tribute_time")
                ));
            }
        } catch (SQLException e) {
            logger.severe("Failed to get liege: " + e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public List<VassalageRelationship> getAllRelationships() {
        List<VassalageRelationship> relationships = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM " + prefix + "vassalage_relationships")) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                relationships.add(new VassalageRelationship(
                        UUID.fromString(rs.getString("liege_uuid")),
                        UUID.fromString(rs.getString("vassal_uuid")),
                        rs.getDouble("tribute_rate"),
                        rs.getLong("established_time"),
                        rs.getLong("last_tribute_time")
                ));
            }

            logger.info("Loaded " + relationships.size() + " vassalage relationships");
        } catch (SQLException e) {
            logger.severe("Failed to get all relationships: " + e.getMessage());
        }

        return relationships;
    }

    // Offer methods
    @Override
    public void saveOffer(VassalageOffer offer) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO " + prefix + "vassalage_offers " +
                             "(offer_uuid, liege_uuid, target_uuid, proposed_tribute_rate, offer_time, expiry_time) " +
                             "VALUES (?, ?, ?, ?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE " +
                             "proposed_tribute_rate = ?, expiry_time = ?")) {

            stmt.setString(1, offer.getOfferUUID().toString());
            stmt.setString(2, offer.getLiegeUUID().toString());
            stmt.setString(3, offer.getTargetUUID().toString());
            stmt.setDouble(4, offer.getProposedTributeRate());
            stmt.setLong(5, offer.getOfferTime());
            stmt.setLong(6, offer.getExpiryTime());
            stmt.setDouble(7, offer.getProposedTributeRate());
            stmt.setLong(8, offer.getExpiryTime());

            stmt.executeUpdate();
            logger.fine("Saved vassalage offer: " + offer.getOfferUUID());
        } catch (SQLException e) {
            logger.severe("Failed to save vassalage offer: " + e.getMessage());
        }
    }

    @Override
    public void removeOffer(UUID offerUUID) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM " + prefix + "vassalage_offers WHERE offer_uuid = ?")) {

            stmt.setString(1, offerUUID.toString());
            int deleted = stmt.executeUpdate();
            logger.fine("Removed vassalage offer: " + offerUUID + " (rows: " + deleted + ")");
        } catch (SQLException e) {
            logger.severe("Failed to remove vassalage offer: " + e.getMessage());
        }
    }

    @Override
    public void removeOffersFrom(UUID liegeUUID) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM " + prefix + "vassalage_offers WHERE liege_uuid = ?")) {

            stmt.setString(1, liegeUUID.toString());
            int deleted = stmt.executeUpdate();
            logger.fine("Removed offers from " + liegeUUID + " (rows: " + deleted + ")");
        } catch (SQLException e) {
            logger.severe("Failed to remove offers from liege: " + e.getMessage());
        }
    }

    @Override
    public void removeOffersTo(UUID targetUUID) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM " + prefix + "vassalage_offers WHERE target_uuid = ?")) {

            stmt.setString(1, targetUUID.toString());
            int deleted = stmt.executeUpdate();
            logger.fine("Removed offers to " + targetUUID + " (rows: " + deleted + ")");
        } catch (SQLException e) {
            logger.severe("Failed to remove offers to target: " + e.getMessage());
        }
    }

    @Override
    public Optional<VassalageOffer> getOffer(UUID offerUUID) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM " + prefix + "vassalage_offers WHERE offer_uuid = ?")) {

            stmt.setString(1, offerUUID.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(new VassalageOffer(
                        UUID.fromString(rs.getString("offer_uuid")),
                        UUID.fromString(rs.getString("liege_uuid")),
                        UUID.fromString(rs.getString("target_uuid")),
                        rs.getDouble("proposed_tribute_rate"),
                        rs.getLong("offer_time"),
                        rs.getLong("expiry_time")
                ));
            }
        } catch (SQLException e) {
            logger.severe("Failed to get vassalage offer: " + e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public List<VassalageOffer> getOffersFrom(UUID liegeUUID) {
        List<VassalageOffer> offers = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM " + prefix + "vassalage_offers WHERE liege_uuid = ?")) {

            stmt.setString(1, liegeUUID.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                offers.add(new VassalageOffer(
                        UUID.fromString(rs.getString("offer_uuid")),
                        UUID.fromString(rs.getString("liege_uuid")),
                        UUID.fromString(rs.getString("target_uuid")),
                        rs.getDouble("proposed_tribute_rate"),
                        rs.getLong("offer_time"),
                        rs.getLong("expiry_time")
                ));
            }
        } catch (SQLException e) {
            logger.severe("Failed to get offers from liege: " + e.getMessage());
        }

        return offers;
    }

    @Override
    public List<VassalageOffer> getOffersTo(UUID targetUUID) {
        List<VassalageOffer> offers = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM " + prefix + "vassalage_offers WHERE target_uuid = ?")) {

            stmt.setString(1, targetUUID.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                offers.add(new VassalageOffer(
                        UUID.fromString(rs.getString("offer_uuid")),
                        UUID.fromString(rs.getString("liege_uuid")),
                        UUID.fromString(rs.getString("target_uuid")),
                        rs.getDouble("proposed_tribute_rate"),
                        rs.getLong("offer_time"),
                        rs.getLong("expiry_time")
                ));
            }
        } catch (SQLException e) {
            logger.severe("Failed to get offers to target: " + e.getMessage());
        }

        return offers;
    }

    @Override
    public List<VassalageOffer> getAllOffers() {
        List<VassalageOffer> offers = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM " + prefix + "vassalage_offers")) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                offers.add(new VassalageOffer(
                        UUID.fromString(rs.getString("offer_uuid")),
                        UUID.fromString(rs.getString("liege_uuid")),
                        UUID.fromString(rs.getString("target_uuid")),
                        rs.getDouble("proposed_tribute_rate"),
                        rs.getLong("offer_time"),
                        rs.getLong("expiry_time")
                ));
            }

            logger.info("Loaded " + offers.size() + " vassalage offers");
        } catch (SQLException e) {
            logger.severe("Failed to get all offers: " + e.getMessage());
        }

        return offers;
    }

    @Override
    public void cleanupExpiredOffers() {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM " + prefix + "vassalage_offers WHERE expiry_time < ?")) {

            stmt.setLong(1, System.currentTimeMillis());
            int deleted = stmt.executeUpdate();
            
            if (deleted > 0) {
                logger.info("Cleaned up " + deleted + " expired vassalage offers");
            }
        } catch (SQLException e) {
            logger.severe("Failed to cleanup expired offers: " + e.getMessage());
        }
    }
}