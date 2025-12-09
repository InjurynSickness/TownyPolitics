package com.orbismc.townyPolitics.listeners;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.managers.VassalageManager;
import com.orbismc.townyPolitics.utils.DelegateLogger;
import com.orbismc.townyPolitics.vassalage.VassalageRelationship;
import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;

/**
 * Handles breaking vassalage relationships when incompatible government types are set
 */
public class GovernmentVassalageListener implements Listener {
    private final TownyPolitics plugin;
    private final VassalageManager vassalageManager;
    private final DelegateLogger logger;

    public GovernmentVassalageListener(TownyPolitics plugin, VassalageManager vassalageManager) {
        this.plugin = plugin;
        this.vassalageManager = vassalageManager;
        this.logger = new DelegateLogger(plugin, "GovernmentVassalageListener");
    }

    /**
     * Custom event handler for government changes
     * Since there's no built-in Towny event for government changes, we'll need to monitor this differently
     * This would be called from the GovernmentManager when governments are changed
     */
    public void onGovernmentTypeChange(Nation nation, GovernmentType oldType, GovernmentType newType) {
        // Check if the new government type is incompatible with vassalage
        if (newType == GovernmentType.TRIBAL || newType == GovernmentType.THEOCRACY) {
            logger.info("Nation " + nation.getName() + " changed to incompatible government type: " + newType);
            
            // Break all vassalage relationships for this nation
            breakVassalageForIncompatibleGovernment(nation, newType);
        }
    }

    private void breakVassalageForIncompatibleGovernment(Nation nation, GovernmentType newType) {
        // Check if nation is a liege
        if (vassalageManager.isLiege(nation)) {
            List<VassalageRelationship> vassals = vassalageManager.getVassals(nation);
            for (VassalageRelationship relationship : vassals) {
                Nation vassal = plugin.getTownyAPI().getNation(relationship.getVassalUUID());
                String vassalName = vassal != null ? vassal.getName() : "Unknown";
                
                vassalageManager.releaseVassal(nation, vassal);
                logger.info("Released vassal " + vassalName + " due to " + nation.getName() + 
                           " changing to incompatible government: " + newType.getDisplayName());
                
                // Notify the vassal nation's online players
                if (vassal != null) {
                    for (org.bukkit.entity.Player player : plugin.getTownyAPI().getOnlinePlayers(vassal)) {
                        player.sendMessage(org.bukkit.ChatColor.YELLOW + 
                                "Your nation has been released from vassalage because " + 
                                nation.getName() + " adopted " + newType.getDisplayName() + " government.");
                    }
                }
            }
        }

        // Check if nation is a vassal
        VassalageRelationship liegeRelationship = vassalageManager.getLiege(nation);
        if (liegeRelationship != null) {
            Nation liege = plugin.getTownyAPI().getNation(liegeRelationship.getLiegeUUID());
            String liegeName = liege != null ? liege.getName() : "Unknown";
            
            // Break vassalage (no authority cost since it's due to government incompatibility)
            vassalageManager.removeRelationship(liegeRelationship.getLiegeUUID(), nation.getUUID());
            logger.info("Broke vassalage between " + liegeName + " and " + nation.getName() + 
                       " due to incompatible government change: " + newType.getDisplayName());
            
            // Notify both nations
            for (org.bukkit.entity.Player player : plugin.getTownyAPI().getOnlinePlayers(nation)) {
                player.sendMessage(org.bukkit.ChatColor.YELLOW + 
                        "Your vassalage with " + liegeName + " has been broken due to adopting " + 
                        newType.getDisplayName() + " government.");
            }
            
            if (liege != null) {
                for (org.bukkit.entity.Player player : plugin.getTownyAPI().getOnlinePlayers(liege)) {
                    player.sendMessage(org.bukkit.ChatColor.YELLOW + 
                            "Your vassal " + nation.getName() + " is no longer under your rule due to " +
                            "adopting " + newType.getDisplayName() + " government.");
                }
            }
        }
    }

    /**
     * Helper method to check if a government type is compatible with vassalage
     */
    public boolean isGovernmentCompatibleWithVassalage(GovernmentType type) {
        return type != GovernmentType.TRIBAL && type != GovernmentType.THEOCRACY;
    }
}