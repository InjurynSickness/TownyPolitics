package com.orbismc.townyPolitics.storage;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.DatabaseManager;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class AbstractMySQLStorage {
    protected final TownyPolitics plugin;
    protected final DatabaseManager dbManager;
    protected final String prefix;
    protected final DelegateLogger logger;

    public AbstractMySQLStorage(TownyPolitics plugin, DatabaseManager dbManager, String loggerPrefix) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.prefix = dbManager.getPrefix();
        this.logger = new DelegateLogger(plugin, loggerPrefix);
    }

    protected Connection getConnection() throws SQLException {
        return dbManager.getConnection();
    }

    public void saveAll() {
        // Default implementation (no action for MySQL)
        logger.fine("saveAll() called (no action needed for MySQL storage)");
    }
}