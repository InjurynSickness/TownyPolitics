package com.orbismc.townyPolitics;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager {

    private final TownyPolitics plugin;
    private final ComponentLogger logger;
    private HikariDataSource dataSource;
    private final String prefix;

    public DatabaseManager(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = plugin.getComponentLogger();
        this.prefix = plugin.getConfig().getString("database.prefix", "tp_");
        setupMySQL();
    }

    private void setupMySQL() {
        String host = plugin.getConfig().getString("database.host", "localhost");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String database = plugin.getConfig().getString("database.database", "townypolitics");
        String username = plugin.getConfig().getString("database.username", "root");
        String password = plugin.getConfig().getString("database.password", "password");
        int poolSize = plugin.getConfig().getInt("database.connection_pool_size", 10);
        long maxLifetime = plugin.getConfig().getLong("database.max_lifetime", 1800000);

        // Modern JDBC URL with better parameters
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database +
                "?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8" +
                "&serverTimezone=UTC&rewriteBatchedStatements=true&cachePrepStmts=true&useServerPrepStmts=true";

        logger.info("Connecting to MySQL at {}:{}/{}", host, port, database);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        config.setMaxLifetime(maxLifetime);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("TownyPolitics-HikariCP");
        config.setLeakDetectionThreshold(60000); // 60 seconds
        
        // Modern HikariCP settings
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(600000); // 10 minutes
        config.setMinimumIdle(Math.min(poolSize / 2, 5));

        try {
            dataSource = new HikariDataSource(config);
            logger.info("Successfully connected to MySQL database");
            createTables();
        } catch (Exception e) {
            logger.error("Failed to connect to MySQL database", e);
        }
    }

    private void createTables() {
        try (Connection conn = getConnection()) {
            // Authority table
            try (PreparedStatement stmt = conn.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS %sauthority (
                        nation_uuid VARCHAR(36) PRIMARY KEY,
                        authority_amount DOUBLE NOT NULL,
                        INDEX idx_authority_amount (authority_amount)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """.formatted(prefix))) {
                stmt.executeUpdate();
                logger.debug("Created/verified authority table");
            }

            // Town Authority table
            try (PreparedStatement stmt = conn.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS %stown_authority (
                        town_uuid VARCHAR(36) PRIMARY KEY,
                        authority_amount DOUBLE NOT NULL,
                        INDEX idx_town_authority_amount (authority_amount)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """.formatted(prefix))) {
                stmt.executeUpdate();
                logger.debug("Created/verified town_authority table");
            }

            // Governments table
            try (PreparedStatement stmt = conn.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS %sgovernments (
                        entity_uuid VARCHAR(36) PRIMARY KEY,
                        entity_type ENUM('TOWN', 'NATION') NOT NULL,
                        government_type VARCHAR(50) NOT NULL,
                        last_change_time BIGINT NOT NULL,
                        INDEX idx_entity_type (entity_type),
                        INDEX idx_government_type (government_type)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """.formatted(prefix))) {
                stmt.executeUpdate();
                logger.debug("Created/verified governments table");
            }

            // Decadence table
            try (PreparedStatement stmt = conn.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS %sdecadence (
                        nation_uuid VARCHAR(36) PRIMARY KEY,
                        decadence_amount DOUBLE NOT NULL,
                        INDEX idx_decadence_amount (decadence_amount)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """.formatted(prefix))) {
                stmt.executeUpdate();
                logger.debug("Created/verified decadence table");
            }

            // Town Decadence table
            try (PreparedStatement stmt = conn.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS %stown_decadence (
                        town_uuid VARCHAR(36) PRIMARY KEY,
                        decadence_amount DOUBLE NOT NULL,
                        INDEX idx_town_decadence_amount (decadence_amount)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """.formatted(prefix))) {
                stmt.executeUpdate();
                logger.debug("Created/verified town_decadence table");
            }

            // Active Policies table
            try (PreparedStatement stmt = conn.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS %sactive_policies (
                        id VARCHAR(36) PRIMARY KEY,
                        policy_id VARCHAR(50) NOT NULL,
                        entity_uuid VARCHAR(36) NOT NULL,
                        is_nation BOOLEAN NOT NULL,
                        enacted_time BIGINT NOT NULL,
                        expiry_time BIGINT NOT NULL,
                        INDEX idx_entity_policies (entity_uuid, is_nation),
                        INDEX idx_expiry_time (expiry_time),
                        INDEX idx_policy_id (policy_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """.formatted(prefix))) {
                stmt.executeUpdate();
                logger.debug("Created/verified active_policies table");
            }

            // Vassalage relationships table
            try (PreparedStatement stmt = conn.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS %svassalage_relationships (
                        liege_uuid VARCHAR(36) NOT NULL,
                        vassal_uuid VARCHAR(36) NOT NULL,
                        tribute_rate DOUBLE NOT NULL,
                        established_time BIGINT NOT NULL,
                        last_tribute_time BIGINT NOT NULL,
                        PRIMARY KEY (liege_uuid, vassal_uuid),
                        INDEX idx_liege (liege_uuid),
                        INDEX idx_vassal (vassal_uuid),
                        INDEX idx_tribute_rate (tribute_rate)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """.formatted(prefix))) {
                stmt.executeUpdate();
                logger.debug("Created/verified vassalage_relationships table");
            }

            // Vassalage offers table
            try (PreparedStatement stmt = conn.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS %svassalage_offers (
                        offer_uuid VARCHAR(36) PRIMARY KEY,
                        liege_uuid VARCHAR(36) NOT NULL,
                        target_uuid VARCHAR(36) NOT NULL,
                        proposed_tribute_rate DOUBLE NOT NULL,
                        offer_time BIGINT NOT NULL,
                        expiry_time BIGINT NOT NULL,
                        INDEX idx_liege_offers (liege_uuid),
                        INDEX idx_target_offers (target_uuid),
                        INDEX idx_expiry (expiry_time)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """.formatted(prefix))) {
                stmt.executeUpdate();
                logger.debug("Created/verified vassalage_offers table");
            }

            logger.info("Successfully created/verified all database tables");
        } catch (SQLException e) {
            logger.error("Failed to create database tables", e);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Database connection is not available");
        }
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing database connection pool");
            dataSource.close();
        }
    }

    public String getPrefix() {
        return prefix;
    }
}