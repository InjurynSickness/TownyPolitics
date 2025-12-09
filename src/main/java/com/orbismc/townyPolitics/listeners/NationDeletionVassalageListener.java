package com.orbismc.townyPolitics.listeners;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.VassalageManager;
import com.orbismc.townyPolitics.utils.DelegateLogger;
import com.orbismc.townyPolitics.vassalage.VassalageRelationship;
import com.palmergames.bukkit.towny.event.DeleteNationEvent;
import com.palmergames.bukkit.towny.event.PreDeleteNationEvent;
import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;

/**
 * Handles immediate cleanup of vassalage relationships when nations are deleted
 */
public class NationDeletionVassalageListener implements Listener {
    private final TownyPolitics plugin;
    private final VassalageManager vassalageManager;
    private final DelegateLogger logger;

    public NationDeletionVassalageListener(TownyPolitics plugin, VassalageManager vassalageManager) {
        this.plugin = plugin;
        this.vassalageManager = vassalageManager;
        this.logger = new DelegateLogger(plugin, "NationDeletionVassalageListener");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreDeleteNation(PreDeleteNationEvent event) {
        Nation deletingNation = event.getNation();
        if (deletingNation == null) {
            return;
        }

        logger.info("Processing vassalage cleanup for deleting nation: " + deletingNation.getName());
        
        // Clean up all vassalage relationships before the nation is actually deleted
        cleanupVassalageRelationships(deletingNation);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNationDelete(DeleteNationEvent event) {
        // Get nation info from the event fields since getNation() doesn't exist
        String nationName = event.getNationName();
        java.util.UUID nationUUID = event.getNationUUID();
        
        if (nationUUID == null) {
            logger.warning("Nation UUID is null for deleted nation: " + nationName);
            return;
        }

        logger.info("Final cleanup for deleted nation: " + nationName);
        
        // Final cleanup using UUID
        vassalageManager.removeAllRelationshipsForNation(nationUUID);
    }

    private void cleanupVassalageRelationships(Nation deletedNation) {
        boolean hadRelationships = false;

        // Check if deleted nation was a liege
        if (vassalageManager.isLiege(deletedNation)) {
            hadRelationships = true;
            List<VassalageRelationship> vassals = vassalageManager.getVassals(deletedNation);
            
            logger.info("Deleted nation " + deletedNation.getName() + " had " + vassals.size() + " vassals");
            
            for (VassalageRelationship relationship : vassals) {
                Nation vassal = plugin.getTownyAPI().getNation(relationship.getVassalUUID());
                if (vassal != null) {
                    // Notify vassal nation's online players
                    for (org.bukkit.entity.Player player : plugin.getTownyAPI().getOnlinePlayers(vassal)) {
                        player.sendMessage(org.bukkit.ChatColor.YELLOW + 
                                "Your overlord nation " + deletedNation.getName() + 
                                " has been dissolved. You are no longer a vassal.");
                    }
                    
                    logger.info("Released vassal " + vassal.getName() + " due to liege nation deletion");
                }
            }
        }

        // Check if deleted nation was a vassal
        VassalageRelationship liegeRelationship = vassalageManager.getLiege(deletedNation);
        if (liegeRelationship != null) {
            hadRelationships = true;
            Nation liege = plugin.getTownyAPI().getNation(liegeRelationship.getLiegeUUID());
            
            if (liege != null) {
                // Notify liege nation's online players
                for (org.bukkit.entity.Player player : plugin.getTownyAPI().getOnlinePlayers(liege)) {
                    player.sendMessage(org.bukkit.ChatColor.YELLOW + 
                            "Your vassal nation " + deletedNation.getName() + 
                            " has been dissolved and is no longer under your rule.");
                }
                
                logger.info("Removed vassal " + deletedNation.getName() + " from liege " + liege.getName() + 
                           " due to vassal nation deletion");
            }
        }

        // Remove all relationships and offers for this nation
        vassalageManager.removeAllRelationshipsForNation(deletedNation.getUUID());
        
        if (hadRelationships) {
            logger.info("Successfully cleaned up all vassalage relationships for deleted nation: " + 
                       deletedNation.getName());
        } else {
            logger.fine("Deleted nation " + deletedNation.getName() + " had no vassalage relationships to clean up");
        }
    }
}