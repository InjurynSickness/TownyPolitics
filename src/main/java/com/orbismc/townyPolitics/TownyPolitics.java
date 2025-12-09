package com.orbismc.townyPolitics;

import com.orbismc.townyPolitics.handlers.PolicyEffectsHandler;
import com.orbismc.townyPolitics.initialization.CommandInitializer;
import com.orbismc.townyPolitics.initialization.ListenerInitializer;
import com.orbismc.townyPolitics.initialization.ManagerInitializer;
import com.palmergames.bukkit.towny.TownyAPI;
import com.orbismc.townyPolitics.managers.*;
import com.orbismc.townyPolitics.storage.IVassalageStorage;
import com.orbismc.townyPolitics.storage.mysql.MySQLVassalageStorage;
import com.orbismc.townyPolitics.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class TownyPolitics extends JavaPlugin implements DailyProcessor {

    private TownyAPI townyAPI;
    private ConfigManager configManager;
    private DebugLogger debugLogger;
    private DatabaseManager databaseManager;

    // Core Managers
    private AuthorityManager authorityManager;
    private GovernmentManager govManager;
    private DecadenceManager decadenceManager;
    private TownGovernmentManager townGovManager;
    private TownDecadenceManager townDecadenceManager;
    private TownAuthorityManager townAuthorityManager;
    private PolicyManager policyManager;
    private PolicyEffectsHandler policyEffectsHandler;
    private TaxationManager taxationManager;
    private VassalageManager vassalageManager;

    @Override
    public void onEnable() {
        // Check dependencies
        if (!checkDependencies()) {
            return;
        }

        townyAPI = TownyAPI.getInstance();

        // Initialize core components
        initializeCoreComponents();

        // Initialize managers
        ManagerInitializer managerInitializer = new ManagerInitializer(this);
        managerInitializer.initialize();

        // Get the initialized managers
        this.authorityManager = managerInitializer.getAuthorityManager();
        this.govManager = managerInitializer.getGovManager();
        this.decadenceManager = managerInitializer.getDecadenceManager();
        this.townGovManager = managerInitializer.getTownGovManager();
        this.townDecadenceManager = managerInitializer.getTownDecadenceManager();
        this.townAuthorityManager = managerInitializer.getTownAuthorityManager();
        this.policyManager = managerInitializer.getPolicyManager();
        this.taxationManager = managerInitializer.getTaxationManager();
        this.policyEffectsHandler = new PolicyEffectsHandler(this);

        // Initialize vassalage manager
        IVassalageStorage vassalageStorage = new MySQLVassalageStorage(this, databaseManager);
        vassalageManager = new VassalageManager(this, vassalageStorage);
        debugLogger.info("Vassalage Manager initialized");

        // Register event listeners
        ListenerInitializer listenerInitializer = new ListenerInitializer(this);
        listenerInitializer.initialize();

        // Register commands
        CommandInitializer commandInitializer = new CommandInitializer(this);
        commandInitializer.initialize();

        debugLogger.info("TownyPolitics (Medieval Edition) has been enabled");
        getLogger().info("TownyPolitics (Medieval Edition) has been enabled");
    }

    private void initializeCoreComponents() {
        // Initialize config and debug logger
        configManager = new ConfigManager(this);
        debugLogger = new DebugLogger(this);
        databaseManager = new DatabaseManager(this);
        debugLogger.info("TownyPolitics debug logger initialized");
    }

    private boolean checkDependencies() {
        if (Bukkit.getPluginManager().getPlugin("Towny") == null) {
            getLogger().severe("Towny not found! Disabling TownyPolitics...");
            Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }
        return true;
    }

    @Override
    public void onDisable() {
        // Save all data on plugin disable
        saveAllData();

        // Close database connection
        if (databaseManager != null) {
            databaseManager.close();
        }

        debugLogger.info("TownyPolitics (Medieval Edition) has been disabled");
        getLogger().info("TownyPolitics (Medieval Edition) has been disabled");
    }

    private void saveAllData() {
        if (authorityManager != null) authorityManager.saveAllData();
        if (govManager != null) govManager.saveAllData();
        if (decadenceManager != null) decadenceManager.saveAllData();
        if (townGovManager != null) townGovManager.saveAllData();
        if (townDecadenceManager != null) townDecadenceManager.saveAllData();
        if (townAuthorityManager != null) townAuthorityManager.saveAllData();
        if (policyManager != null) policyManager.saveAllData();
        if (vassalageManager != null) vassalageManager.saveAllData();
    }

    public void reload() {
        // Reload configuration
        getConfigManager().loadConfig();

        // Reinitialize debugLogger with new config settings
        debugLogger = new DebugLogger(this);
        debugLogger.info("Debug logger reinitialized with new config settings");

        // Reload all managers
        if (authorityManager != null) authorityManager.loadData();
        if (govManager != null) govManager.loadData();
        if (decadenceManager != null) decadenceManager.loadData();
        if (townGovManager != null) townGovManager.loadData();
        if (townDecadenceManager != null) townDecadenceManager.loadData();
        if (townAuthorityManager != null) townAuthorityManager.loadData();
        if (policyManager != null) policyManager.reload();
        if (vassalageManager != null) vassalageManager.loadData();

        debugLogger.info("Configuration reloaded");
    }

    @Override
    public void processNewDay() {
        debugLogger.info("Processing daily updates");

        // Use a null-safe approach when calling
        if (authorityManager != null) authorityManager.processNewDay();
        if (decadenceManager != null) decadenceManager.processNewDay();
        if (townDecadenceManager != null) townDecadenceManager.processNewDay();
        if (townAuthorityManager != null) townAuthorityManager.processNewDay();
        if (policyManager != null) policyManager.processNewDay();
        if (vassalageManager != null) vassalageManager.processNewDay();

        debugLogger.info("Daily updates complete");
    }

    // Getters
    public TownyAPI getTownyAPI() { return townyAPI; }
    public ConfigManager getConfigManager() { return configManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public AuthorityManager getAuthorityManager() { return authorityManager; }
    public GovernmentManager getGovManager() { return govManager; }
    public DecadenceManager getDecadenceManager() { return decadenceManager; }
    public TownGovernmentManager getTownGovManager() { return townGovManager; }
    public TownDecadenceManager getTownDecadenceManager() { return townDecadenceManager; }
    public TownAuthorityManager getTownAuthorityManager() { return townAuthorityManager; }
    public PolicyManager getPolicyManager() { return policyManager; }
    public TaxationManager getTaxationManager() { return taxationManager; }
    public DebugLogger getDebugLogger() { return debugLogger; }
    public PolicyEffectsHandler getPolicyEffectsHandler() { return policyEffectsHandler; }
    public VassalageManager getVassalageManager() { return vassalageManager; }
}