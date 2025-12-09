package com.orbismc.townyPolitics.initialization;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.*;
import com.orbismc.townyPolitics.storage.*;
import com.orbismc.townyPolitics.storage.mysql.MySQLVassalageStorage;
import com.orbismc.townyPolitics.utils.DelegateLogger;

public class ManagerInitializer {
    private final TownyPolitics plugin;
    private final DelegateLogger logger;

    // Managers
    private AuthorityManager authorityManager;
    private GovernmentManager govManager;
    private DecadenceManager decadenceManager;
    private TownGovernmentManager townGovManager;
    private TownDecadenceManager townDecadenceManager;
    private TownAuthorityManager townAuthorityManager;
    private TaxationManager taxationManager;
    private PolicyManager policyManager;
    private VassalageManager vassalageManager;

    public ManagerInitializer(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "ManagerInit");
    }

    public void initialize() {
        StorageInitializer storageInit = new StorageInitializer(plugin, plugin.getDatabaseManager());
        storageInit.initialize();

        // Initialize core managers with unified storage
        govManager = new GovernmentManager(plugin, storageInit.getNationStorage());
        authorityManager = new AuthorityManager(plugin, storageInit.getNationStorage(), govManager);
        decadenceManager = new DecadenceManager(plugin, storageInit.getNationStorage(), govManager);

        // Initialize town managers
        townGovManager = new TownGovernmentManager(plugin, storageInit.getTownStorage());
        townDecadenceManager = new TownDecadenceManager(plugin, storageInit.getTownStorage(), townGovManager);
        townAuthorityManager = new TownAuthorityManager(plugin, storageInit.getTownStorage(), townGovManager);
        logger.info("Town Authority Manager initialized");

        // Initialize taxation manager
        taxationManager = new TaxationManager(plugin, decadenceManager);

        // Initialize policy manager
        policyManager = new PolicyManager(plugin, storageInit.getPolicyStorage(), govManager, townGovManager);
        logger.info("Policy Manager initialized");

        // Initialize vassalage manager
        IVassalageStorage vassalageStorage = new MySQLVassalageStorage(plugin, plugin.getDatabaseManager());
        vassalageManager = new VassalageManager(plugin, vassalageStorage);
        logger.info("Vassalage Manager initialized");

        logger.info("All managers initialized with unified storage");
    }

    // Getters for all the managers
    public AuthorityManager getAuthorityManager() { return authorityManager; }
    public GovernmentManager getGovManager() { return govManager; }
    public DecadenceManager getDecadenceManager() { return decadenceManager; }
    public TownGovernmentManager getTownGovManager() { return townGovManager; }
    public TownDecadenceManager getTownDecadenceManager() { return townDecadenceManager; }
    public TownAuthorityManager getTownAuthorityManager() { return townAuthorityManager; }
    public TaxationManager getTaxationManager() { return taxationManager; }
    public PolicyManager getPolicyManager() { return policyManager; }
    public VassalageManager getVassalageManager() { return vassalageManager; }
}