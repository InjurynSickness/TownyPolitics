package com.orbismc.townyPolitics.managers;

import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.storage.IEntityStorage;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TownAuthorityManager implements Manager {

    private final TownyPolitics plugin;
    private final IEntityStorage<Town> storage;
    private final TownGovernmentManager townGovManager;
    private final Map<UUID, Double> townAuthority; // Cache of town UUIDs to their authority
    private final DelegateLogger logger;

    // Maximum authority limit for towns
    private final double MAX_AUTHORITY = 500.0;

    public TownAuthorityManager(TownyPolitics plugin, IEntityStorage<Town> storage, TownGovernmentManager townGovManager) {
        this.plugin = plugin;
        this.storage = storage;
        this.townGovManager = townGovManager;
        this.townAuthority = new HashMap<>();
        this.logger = new DelegateLogger(plugin, "TownAuthorityManager");

        // Load data from storage
        loadData();
    }

    @Override
    public void loadData() {
        townAuthority.clear();
        townAuthority.putAll(storage.loadAllAuthority());
        logger.info("Loaded authority data for " + townAuthority.size() + " towns");
    }

    @Override
    public void saveAllData() {
        storage.saveAll();
        logger.info("Saved town authority data to storage");
    }

    public double getAuthority(Town town) {
        double authority = townAuthority.getOrDefault(town.getUUID(), 0.0);
        logger.fine("Town " + town.getName() + " has " + authority + " authority");
        return authority;
    }

    public void setAuthority(Town town, double amount) {
        // Ensure authority can't go below 0 or above MAX_AUTHORITY
        double newAmount = Math.min(MAX_AUTHORITY, Math.max(0, amount));
        double oldAmount = townAuthority.getOrDefault(town.getUUID(), 0.0);

        townAuthority.put(town.getUUID(), newAmount);
        storage.saveAuthority(town.getUUID(), newAmount);

        logger.info("Town " + town.getName() + " authority set to " +
                newAmount + " (was " + oldAmount + ")");
    }

    public void addAuthority(Town town, double amount) {
        double current = getAuthority(town);
        double newAmount = current + amount;

        logger.info("Adding " + amount + " authority to town " +
                town.getName() + " (new total: " + newAmount + ")");

        setAuthority(town, newAmount);
    }

    public boolean removeAuthority(Town town, double amount) {
        double current = getAuthority(town);
        if (current >= amount) {
            double newAmount = current - amount;

            logger.info("Removing " + amount + " authority from town " +
                    town.getName() + " (new total: " + newAmount + ")");

            setAuthority(town, newAmount);
            return true;
        }

        logger.warning("Cannot remove " + amount + " authority from town " +
                town.getName() + " (only has " + current + ")");
        return false;
    }

    public double getMaxAuthority() {
        return MAX_AUTHORITY;
    }

    public double calculateDailyAuthorityGain(Town town) {
        int residents = town.getResidents().size();
        double baseGain = plugin.getConfig().getDouble("town_authority.base_gain", 0.5);
        double maxGain = plugin.getConfig().getDouble("town_authority.max_daily_gain", 3.0);
        double minGain = plugin.getConfig().getDouble("town_authority.min_daily_gain", 0.5);

        double authorityGain;
        if (residents <= 0) {
            authorityGain = 0; // No residents, no authority
        } else if (residents == 1) {
            authorityGain = baseGain; // 1 resident = base_gain authority
        } else if (residents <= 5) {
            authorityGain = baseGain + (residents - 1) * 0.125 * baseGain;
        } else if (residents <= 10) {
            authorityGain = 1.5 * baseGain + (residents - 5) * 0.1 * baseGain;
        } else {
            authorityGain = Math.min(maxGain, 2.0 * baseGain + Math.log10(residents / 10.0) * 2 * baseGain);
        }

        // Apply government type modifier
        GovernmentType govType = townGovManager.getGovernmentType(town);
        double govModifier = govType.getAuthorityModifier();
        authorityGain *= govModifier;

        // Apply decadence modifiers
        TownDecadenceManager townDecadenceManager = plugin.getTownDecadenceManager();
        if (townDecadenceManager != null) {
            double authorityModifier = 1.0; // Default modifier
            int decadenceLevel = townDecadenceManager.getDecadenceThresholdLevel(town);

            // Apply modifiers based on decadence level (similar to nation system)
            if (decadenceLevel >= 2) { // Medium+ decadence affects authority gain
                if (decadenceLevel == 2) authorityModifier = 0.90; // -10% authority gain
                else if (decadenceLevel == 3) authorityModifier = 0.75; // -25% authority gain
                else if (decadenceLevel == 4) authorityModifier = 0.50; // -50% authority gain
            }

            authorityGain *= authorityModifier;
        }

        // Apply policy modifiers
        if (plugin.getPolicyEffectsHandler() != null) {
            double policyModifier = plugin.getPolicyEffectsHandler().getAuthorityGainModifier(town);
            authorityGain *= policyModifier;
        }

        // Apply nation bonus - towns in nations with authority get a small bonus
        if (town.hasNation()) {
            try {
                double nationBonus = plugin.getConfig().getDouble("town_authority.nation_bonus", 0.1);
                authorityGain *= (1.0 + nationBonus);
            } catch (Exception e) {
                logger.warning("Error applying nation bonus: " + e.getMessage());
            }
        }

        double finalGain = Math.min(maxGain, Math.max(minGain, authorityGain));

        logger.fine("Town " + town.getName() + " daily authority gain calculation: " +
                "residents=" + residents + ", baseGain=" + baseGain +
                ", govMod=" + govModifier + ", finalGain=" + finalGain);

        return finalGain;
    }

    public void processNewDay() {
        logger.info("Processing daily authority gains for all towns");

        plugin.getTownyAPI().getTowns().forEach(town -> {
            try {
                double gain = calculateDailyAuthorityGain(town);
                addAuthority(town, gain);
                logger.info("Town " + town.getName() + " gained " +
                        String.format("%.2f", gain) + " authority");
            } catch (Exception e) {
                logger.severe("Error processing authority for town " + town.getName() + ": " + e.getMessage());
            }
        });
    }
}