package com.orbismc.townyPolitics.initialization;

import org.bukkit.plugin.PluginManager;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.handlers.*;
import com.orbismc.townyPolitics.hooks.*;
import com.orbismc.townyPolitics.listeners.*;
import com.orbismc.townyPolitics.utils.DelegateLogger;

public class ListenerInitializer {
    private final TownyPolitics plugin;
    private final DelegateLogger logger;

    public ListenerInitializer(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "ListenerInit");
    }

    public void initialize() {
        PluginManager pm = plugin.getServer().getPluginManager();

        // Register status screen listener
        StatusScreenListener statusListener = new StatusScreenListener(plugin);
        pm.registerEvents(statusListener, plugin);
        logger.info("Status Screen Listener registered");

        // Register core Towny event listener for new day processing
        CoreTownyEventListener coreListener = new CoreTownyEventListener(plugin);
        pm.registerEvents(coreListener, plugin);
        logger.info("Core Towny Event Listener registered");

        // Transaction handlers
        TransactionEmbezzlementHandler embezzlementHandler = new TransactionEmbezzlementHandler(plugin);
        pm.registerEvents(embezzlementHandler, plugin);
        logger.info("Transaction Embezzlement Handler registered");

        // Diagnostic handler for debugging
        DiagnosticTransactionHandler diagnosticHandler = new DiagnosticTransactionHandler(plugin);
        pm.registerEvents(diagnosticHandler, plugin);
        logger.info("Diagnostic Transaction Handler registered");

        // Vassalage-related listeners
        if (plugin.getVassalageManager() != null) {
            // Register enemy protection listener for vassals
            VassalageEnemyListener enemyListener = new VassalageEnemyListener(plugin, plugin.getVassalageManager());
            pm.registerEvents(enemyListener, plugin);
            logger.info("Vassalage Enemy Protection Listener registered");

            // Register nation deletion cleanup listener
            NationDeletionVassalageListener deletionListener = new NationDeletionVassalageListener(plugin, plugin.getVassalageManager());
            pm.registerEvents(deletionListener, plugin);
            logger.info("Nation Deletion Vassalage Cleanup Listener registered");

            // Register government change vassalage listener
            GovernmentVassalageListener governmentListener = new GovernmentVassalageListener(plugin, plugin.getVassalageManager());
            pm.registerEvents(governmentListener, plugin);
            
            // Set the government listener in the government manager for notifications
            if (plugin.getGovManager() != null) {
                plugin.getGovManager().setVassalageListener(governmentListener);
            }
            logger.info("Government Change Vassalage Listener registered");

            // Register economic handler for tribute collection
            VassalageEconomicHandler economicHandler = new VassalageEconomicHandler(plugin, plugin.getVassalageManager());
            pm.registerEvents(economicHandler, plugin);
            logger.info("Vassalage Economic Handler registered");
        } else {
            logger.warning("VassalageManager not available - vassalage listeners not registered");
        }

        logger.info("All event listeners registered");
    }
}