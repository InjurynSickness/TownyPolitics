package com.orbismc.townyPolitics.listeners;

import com.palmergames.bukkit.towny.event.NewDayEvent;
import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.utils.DelegateLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Handles core Towny events like the daily timer
 */
public class CoreTownyEventListener implements Listener {

    private final TownyPolitics plugin;
    private final DelegateLogger logger;

    public CoreTownyEventListener(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "CoreEvents");
    }

    @EventHandler
    public void onNewDay(NewDayEvent event) {
        logger.info("Processing new day for political power, corruption, and policies...");

        // Process daily updates for all components
        plugin.processNewDay();

        logger.info("Political power, corruption, and policy processing complete.");
    }
}