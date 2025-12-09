package com.orbismc.townyPolitics.managers;

import com.palmergames.bukkit.towny.object.Nation;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.listeners.GovernmentVassalageListener;
import com.orbismc.townyPolitics.storage.IEntityStorage;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GovernmentManager implements Manager {

    private final TownyPolitics plugin;
    private final IEntityStorage<Nation> storage;
    private final DelegateLogger logger;

    private Map<UUID, GovernmentType> nationGovernments;

    // Maps to track the last time a government change occurred
    private Map<UUID, Long> nationChangeTimes;

    // Reference to the vassalage listener for government change notifications
    private GovernmentVassalageListener vassalageListener;

    public GovernmentManager(TownyPolitics plugin, IEntityStorage<Nation> storage) {
        this.plugin = plugin;
        this.storage = storage;
        this.logger = new DelegateLogger(plugin, "GovManager");

        this.nationGovernments = new HashMap<>();
        this.nationChangeTimes = new HashMap<>();

        // Load data from storage
        loadData();
    }

    /**
     * Set the vassalage listener for government change notifications
     * This should be called after the VassalageManager is initialized
     */
    public void setVassalageListener(GovernmentVassalageListener listener) {
        this.vassalageListener = listener;
    }

    @Override
    public void loadData() {
        nationGovernments.clear();
        nationChangeTimes.clear();

        nationGovernments.putAll(storage.loadAllGovernments());
        nationChangeTimes.putAll(storage.loadAllChangeTimes());

        logger.info("Loaded " + nationGovernments.size() + " nation governments");
    }

    @Override
    public void saveAllData() {
        storage.saveAll();
        logger.info("Saved government data to storage");
    }

    public void reload() {
        logger.info("Reloading government data");
        loadData();
    }

    public GovernmentType getGovernmentType(Nation nation) {
        GovernmentType type = nationGovernments.getOrDefault(nation.getUUID(), GovernmentType.TRIBAL);
        logger.fine("Nation " + nation.getName() + " has government type: " + type.name());
        return type;
    }

    public long getLastChangeTime(Nation nation) {
        return nationChangeTimes.getOrDefault(nation.getUUID(), 0L);
    }

    public boolean isOnCooldown(Nation nation) {
        long lastChange = getLastChangeTime(nation);
        if (lastChange == 0) {
            return false; // Never changed before
        }

        long cooldownDays = plugin.getConfig().getLong("government.change_cooldown", 30);
        long cooldownMillis = cooldownDays * 24 * 60 * 60 * 1000;
        boolean onCooldown = (System.currentTimeMillis() - lastChange) < cooldownMillis;

        if (onCooldown) {
            logger.info("Nation " + nation.getName() + " is on government change cooldown");
        }

        return onCooldown;
    }

    public long getCooldownTimeRemaining(Nation nation) {
        if (!isOnCooldown(nation)) {
            return 0;
        }

        long lastChange = getLastChangeTime(nation);
        long cooldownDays = plugin.getConfig().getLong("government.change_cooldown", 30);
        long cooldownMillis = cooldownDays * 24 * 60 * 60 * 1000;
        return cooldownMillis - (System.currentTimeMillis() - lastChange);
    }

    public String formatCooldownTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        hours = hours % 24;

        return days + " days, " + hours + " hours";
    }

    public boolean setGovernmentType(Nation nation, GovernmentType type, boolean bypassCooldown) {
        // Check cooldown if not bypassing
        if (!bypassCooldown) {
            if (isOnCooldown(nation)) {
                logger.info("Nation " + nation.getName() + " attempted to change government while on cooldown");
                return false;
            }

            // Get the nation's previous government changes
            long lastChange = getLastChangeTime(nation);
            boolean hasChangedBefore = lastChange > 0;

            // If this is a subsequent change, use the switch_time instead of the full cooldown
            if (hasChangedBefore) {
                long switchTimeDays = plugin.getConfig().getLong("government.switch_time", 7);
                long switchTimeMillis = switchTimeDays * 24 * 60 * 60 * 1000;

                // Check if enough time has passed since the last change
                if (System.currentTimeMillis() - lastChange < switchTimeMillis) {
                    logger.info("Nation " + nation.getName() +
                            " must wait " + formatCooldownTime(switchTimeMillis - (System.currentTimeMillis() - lastChange)) +
                            " before completing government transition");
                    return false;
                }
            }
        }

        // Get old government type for logging and vassalage checks
        GovernmentType oldType = getGovernmentType(nation);

        // Update government type
        nationGovernments.put(nation.getUUID(), type);
        storage.saveGovernment(nation.getUUID(), type);

        // Update change time
        long now = System.currentTimeMillis();
        nationChangeTimes.put(nation.getUUID(), now);
        storage.saveChangeTime(nation.getUUID(), now);

        logger.info("Nation " + nation.getName() + " changed government from " + oldType.name() +
                " to " + type.name() + (bypassCooldown ? " (bypass cooldown)" : ""));

        // Notify vassalage listener of government change
        if (vassalageListener != null && oldType != type) {
            vassalageListener.onGovernmentTypeChange(nation, oldType, type);
        }

        return true;
    }

    public boolean setGovernmentType(Nation nation, GovernmentType type) {
        return setGovernmentType(nation, type, false);
    }

    public boolean hasDemocraticGovernment(Nation nation) {
        GovernmentType type = getGovernmentType(nation);
        return type.isDemocracy();
    }

    /**
     * Check if a government change would break vassalage relationships
     * This can be used to warn players before they change government
     */
    public boolean wouldBreakVassalage(Nation nation, GovernmentType newType) {
        if (newType == GovernmentType.TRIBAL || newType == GovernmentType.THEOCRACY) {
            // Check if nation has any vassalage relationships
            VassalageManager vassalageManager = plugin.getVassalageManager();
            if (vassalageManager != null) {
                return vassalageManager.isLiege(nation) || vassalageManager.isVassal(nation);
            }
        }
        return false;
    }

    /**
     * Get a warning message for government changes that would break vassalage
     */
    public String getVassalageBreakWarning(Nation nation, GovernmentType newType) {
        VassalageManager vassalageManager = plugin.getVassalageManager();
        if (vassalageManager == null) {
            return null;
        }

        if (newType == GovernmentType.TRIBAL || newType == GovernmentType.THEOCRACY) {
            boolean isLiege = vassalageManager.isLiege(nation);
            boolean isVassal = vassalageManager.isVassal(nation);

            if (isLiege && isVassal) {
                return "Warning: Changing to " + newType.getDisplayName() +
                       " will break all your vassalage relationships (both as liege and vassal).";
            } else if (isLiege) {
                int vassalCount = vassalageManager.getVassals(nation).size();
                return "Warning: Changing to " + newType.getDisplayName() +
                       " will release all " + vassalCount + " of your vassals.";
            } else if (isVassal) {
                return "Warning: Changing to " + newType.getDisplayName() +
                       " will break your vassalage with your overlord.";
            }
        }

        return null;
    }
}