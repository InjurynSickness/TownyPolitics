package com.orbismc.townyPolitics.components;

import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.utils.DelegateLogger;
import com.palmergames.adventure.text.Component;

/**
 * Base class for all status screen components
 */
public abstract class StatusComponent {

    protected final TownyPolitics plugin;
    protected final DelegateLogger logger;

    public StatusComponent(TownyPolitics plugin, String loggerSuffix) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "Component" + loggerSuffix);
    }

    /**
     * Add this component to a nation status screen
     */
    public abstract void addToNationScreen(NationStatusScreenEvent event, Nation nation);

    /**
     * Add this component to a town status screen
     */
    public abstract void addToTownScreen(TownStatusScreenEvent event, Town town);

    /**
     * Get the color for a modifier value (green if positive, red if negative)
     */
    protected com.palmergames.adventure.text.format.NamedTextColor getTextColorForValue(double value, boolean isSpendingMod) {
        if (isSpendingMod) {
            // For spending modifiers, positive values are bad (more expensive)
            if (value > 0) return com.palmergames.adventure.text.format.NamedTextColor.RED;
            return com.palmergames.adventure.text.format.NamedTextColor.GREEN;
        } else {
            // For all other modifiers, negative values are bad
            if (value < 0) return com.palmergames.adventure.text.format.NamedTextColor.RED;
            return com.palmergames.adventure.text.format.NamedTextColor.GREEN;
        }
    }

    /**
     * Add a component to a status screen
     */
    protected void addComponentToScreen(Object event, String key, Component component) {
        if (event instanceof NationStatusScreenEvent) {
            ((NationStatusScreenEvent) event).getStatusScreen().addComponentOf(key, component);
        } else if (event instanceof TownStatusScreenEvent) {
            ((TownStatusScreenEvent) event).getStatusScreen().addComponentOf(key, component);
        }
    }
}