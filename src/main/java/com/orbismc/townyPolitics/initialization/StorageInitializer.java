package com.orbismc.townyPolitics.initialization;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.DatabaseManager;
import com.orbismc.townyPolitics.storage.*;
import com.orbismc.townyPolitics.storage.mysql.*;
import com.orbismc.townyPolitics.utils.DelegateLogger;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;

public class StorageInitializer {
    private final TownyPolitics plugin;
    private final DatabaseManager dbManager;
    private final DelegateLogger logger;

    // Unified storage interfaces
    private IEntityStorage<Nation> nationStorage;
    private IEntityStorage<Town> townStorage;
    private IPolicyStorage policyStorage;

    public StorageInitializer(TownyPolitics plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.logger = new DelegateLogger(plugin, "StorageInit");
    }

    public void initialize() {
        // Initialize unified MySQL storage implementations
        nationStorage = new MySQLNationStorage(plugin, dbManager);
        townStorage = new MySQLTownStorage(plugin, dbManager);
        policyStorage = new MySQLPolicyStorage(plugin, dbManager);

        logger.info("Unified storage system initialized");
    }

    // Unified getters
    public IEntityStorage<Nation> getNationStorage() { return nationStorage; }
    public IEntityStorage<Town> getTownStorage() { return townStorage; }
    public IPolicyStorage getPolicyStorage() { return policyStorage; }
}