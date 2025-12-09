package com.orbismc.townyPolitics.listeners;

import com.orbismc.townyPolitics.components.*;
import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Handles all status screen components for towns and nations
 */
public class StatusScreenListener implements Listener {

    private final TownyPolitics plugin;
    private final DelegateLogger logger;

    // Components
    private final PoliticalPowerComponent authorityComponent;
    private final GovernmentComponent govComponent;
    private final CorruptionComponent decadenceComponent;
    private final PolicyComponent policyComponent;
    private final VassalageComponent vassalageComponent;

    public StatusScreenListener(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "StatusScreen");

        // Initialize components
        this.authorityComponent = new PoliticalPowerComponent(plugin);
        this.govComponent = new GovernmentComponent(plugin);
        this.decadenceComponent = new CorruptionComponent(plugin);
        this.policyComponent = new PolicyComponent(plugin);
        this.vassalageComponent = new VassalageComponent(plugin, plugin.getVassalageManager());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onNationStatusScreenEarly(NationStatusScreenEvent event) {
        // Clear any default components that we want to replace
        if (authorityComponent != null) {
            authorityComponent.updateNationAuthorityMetadata(event.getNation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNationStatusScreen(NationStatusScreenEvent event) {
        try {
            Nation nation = event.getNation();

            // Add each component - Use null checks for safety
            if (authorityComponent != null) authorityComponent.addToNationScreen(event, nation);
            if (govComponent != null) govComponent.addToNationScreen(event, nation);
            if (decadenceComponent != null) decadenceComponent.addToNationScreen(event, nation);
            if (policyComponent != null) policyComponent.addToNationScreen(event, nation);
            if (vassalageComponent != null) vassalageComponent.addToNationScreen(event, nation);

        } catch (Exception e) {
            logger.severe("Error adding components to nation status screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTownStatusScreen(TownStatusScreenEvent event) {
        try {
            Town town = event.getTown();

            // Add each component - Use null checks for safety
            if (govComponent != null) govComponent.addToTownScreen(event, town);
            if (decadenceComponent != null) decadenceComponent.addToTownScreen(event, town);
            if (policyComponent != null) policyComponent.addToTownScreen(event, town);

            // Only add town authority component if it's enabled
            if (plugin.getTownAuthorityManager() != null && authorityComponent != null) {
                authorityComponent.addToTownScreen(event, town);
            }

        } catch (Exception e) {
            logger.severe("Error adding components to town status screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
}