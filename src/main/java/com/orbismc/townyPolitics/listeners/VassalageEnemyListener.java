package com.orbismc.townyPolitics.listeners;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.VassalageManager;
import com.orbismc.townyPolitics.utils.DelegateLogger;
import com.orbismc.townyPolitics.vassalage.VassalageRelationship;
import com.palmergames.bukkit.towny.event.NationPreAddEnemyEvent;
import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Handles protection of vassals from being enemied directly
 * Forces nations to enemy the overlord instead of the vassal
 */
public class VassalageEnemyListener implements Listener {
    private final TownyPolitics plugin;
    private final VassalageManager vassalageManager;
    private final DelegateLogger logger;

    public VassalageEnemyListener(TownyPolitics plugin, VassalageManager vassalageManager) {
        this.plugin = plugin;
        this.vassalageManager = vassalageManager;
        this.logger = new DelegateLogger(plugin, "VassalageEnemyListener");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onNationPreAddEnemy(NationPreAddEnemyEvent event) {
        Nation enemyingNation = event.getNation();
        Nation targetNation = event.getEnemy();

        // Check if target nation is a vassal
        VassalageRelationship vassalRelationship = vassalageManager.getLiege(targetNation);
        if (vassalRelationship == null) {
            return; // Not a vassal, allow normal enemying
        }

        Nation overlord = plugin.getTownyAPI().getNation(vassalRelationship.getLiegeUUID());
        if (overlord == null) {
            logger.warning("Overlord nation not found for vassal " + targetNation.getName());
            return; // Overlord no longer exists, allow enemying
        }

        // Check if the enemying nation is already an enemy of the overlord
        if (enemyingNation.getEnemies().contains(overlord)) {
            // Already enemies with overlord, allow enemying the vassal
            logger.info(enemyingNation.getName() + " is already at war with overlord " + overlord.getName() + 
                       ", allowing them to enemy vassal " + targetNation.getName());
            return;
        }

        // Check if enemying nation IS the overlord (overlords can enemy their own vassals to break vassalage)
        if (enemyingNation.getUUID().equals(overlord.getUUID())) {
            logger.info("Overlord " + overlord.getName() + " is enemying their own vassal " + 
                       targetNation.getName() + ", allowing to break vassalage");
            return;
        }

        // NOT enemies with overlord - block the enemying and force them to enemy overlord first
        event.setCancelled(true);
        event.setCancelMessage("Cannot enemy " + targetNation.getName() + " directly as they are a vassal of " + 
                overlord.getName() + ". You must enemy " + overlord.getName() + " first.");

        logger.info(enemyingNation.getName() + " attempted to enemy vassal " + targetNation.getName() + 
                   " but was blocked (overlord: " + overlord.getName() + " - not currently enemies)");
    }
}