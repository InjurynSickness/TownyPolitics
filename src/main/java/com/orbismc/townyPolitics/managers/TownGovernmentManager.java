package com.orbismc.townyPolitics.managers;

import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.storage.IEntityStorage;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TownGovernmentManager implements Manager {

    private final TownyPolitics plugin;
    private final IEntityStorage<Town> storage;
    private final DelegateLogger logger;

    private Map<UUID, GovernmentType> townGovernments;
    private Map<UUID, Long> townChangeTimes;

    public TownGovernmentManager(TownyPolitics plugin, IEntityStorage<Town> storage) {
        this.plugin = plugin;
        this.storage = storage;
        this.logger = new DelegateLogger(plugin, "TownGovManager");

        this.townGovernments = new HashMap<>();
        this.townChangeTimes = new HashMap<>();

        // Load data from storage
        loadData();
    }

    @Override
    public void loadData() {
        townGovernments.clear();
        townChangeTimes.clear();

        townGovernments.putAll(storage.loadAllGovernments());
        townChangeTimes.putAll(storage.loadAllChangeTimes());

        logger.info("Loaded " + townGovernments.size() + " town governments");
    }

    @Override
    public void saveAllData() {
        storage.saveAll();
        logger.info("Saved town government data to storage");
    }

    public void reload() {
        logger.info("Reloading town government data");
        loadData();
    }

    public GovernmentType getGovernmentType(Town town) {
        GovernmentType type = townGovernments.getOrDefault(town.getUUID(), GovernmentType.getDefaultTownGovernment());
        logger.fine("Town " + town.getName() + " has government type: " + type.name());
        return type;
    }

    public long getLastChangeTime(Town town) {
        return townChangeTimes.getOrDefault(town.getUUID(), 0L);
    }

    public boolean isOnCooldown(Town town) {
        long lastChange = getLastChangeTime(town);
        if (lastChange == 0) {
            return false; // Never changed before
        }

        long cooldownDays = plugin.getConfig().getLong("town_government.change_cooldown", 15);
        long cooldownMillis = cooldownDays * 24 * 60 * 60 * 1000;
        boolean onCooldown = (System.currentTimeMillis() - lastChange) < cooldownMillis;

        if (onCooldown) {
            logger.info("Town " + town.getName() + " is on government change cooldown");
        }

        return onCooldown;
    }

    public long getCooldownTimeRemaining(Town town) {
        if (!isOnCooldown(town)) {
            return 0;
        }

        long lastChange = getLastChangeTime(town);
        long cooldownDays = plugin.getConfig().getLong("town_government.change_cooldown", 15);
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

    public boolean setGovernmentType(Town town, GovernmentType type, boolean bypassCooldown) {
        // Check if the government type is valid for towns
        if (!type.isValidForTowns()) {
            logger.warning("Government type " + type.name() + " is not valid for towns");
            return false;
        }

        // Check cooldown if not bypassing
        if (!bypassCooldown) {
            if (isOnCooldown(town)) {
                logger.info("Town " + town.getName() + " attempted to change government while on cooldown");
                return false;
            }

            // Get the town's previous government changes
            long lastChange = getLastChangeTime(town);
            boolean hasChangedBefore = lastChange > 0;

            // If this is a subsequent change, use the switch_time instead of the full cooldown
            if (hasChangedBefore) {
                long switchTimeDays = plugin.getConfig().getLong("government.switch_time", 7);
                long switchTimeMillis = switchTimeDays * 24 * 60 * 60 * 1000;

                // Check if enough time has passed since the last change
                if (System.currentTimeMillis() - lastChange < switchTimeMillis) {
                    logger.info("Town " + town.getName() +
                            " must wait " + formatCooldownTime(switchTimeMillis - (System.currentTimeMillis() - lastChange)) +
                            " before completing government transition");
                    return false;
                }
            }
        }

        // Get old government type for logging
        GovernmentType oldType = getGovernmentType(town);

        // Update government type
        townGovernments.put(town.getUUID(), type);
        storage.saveGovernment(town.getUUID(), type);

        // Update change time
        long now = System.currentTimeMillis();
        townChangeTimes.put(town.getUUID(), now);
        storage.saveChangeTime(town.getUUID(), now);

        logger.info("Town " + town.getName() + " changed government from " + oldType.name() +
                " to " + type.name() + (bypassCooldown ? " (bypass cooldown)" : ""));

        return true;
    }

    public boolean setGovernmentType(Town town, GovernmentType type) {
        return setGovernmentType(town, type, false);
    }

    public boolean hasDemocraticGovernment(Town town) {
        GovernmentType type = getGovernmentType(town);
        return type.isDemocracy();
    }
}