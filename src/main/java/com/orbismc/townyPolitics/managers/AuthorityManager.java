package com.orbismc.townyPolitics.managers;

import com.orbismc.townyPolitics.storage.IEntityStorage;
import com.palmergames.bukkit.towny.object.Nation;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthorityManager implements Manager {

    private final TownyPolitics plugin;
    private final IEntityStorage<Nation> storage;
    private final GovernmentManager govManager;
    private final Map<UUID, Double> nationAuthority; // Cache of nation UUIDs to their authority
    private final DelegateLogger logger;

    // Maximum authority limit
    private final double MAX_AUTHORITY = 1000.0;

    public AuthorityManager(TownyPolitics plugin, IEntityStorage<Nation> storage, GovernmentManager govManager) {
        this.plugin = plugin;
        this.storage = storage;
        this.govManager = govManager;
        this.nationAuthority = new HashMap<>();
        this.logger = new DelegateLogger(plugin, "AuthorityManager");

        // Load data from storage
        loadData();
    }

    @Override
    public void loadData() {
        nationAuthority.clear();
        nationAuthority.putAll(storage.loadAllAuthority());
        logger.info("Loaded authority data for " + nationAuthority.size() + " nations");
    }

    @Override
    public void saveAllData() {
        storage.saveAll();
        logger.info("Saved authority data to storage");
    }

    public double getAuthority(Nation nation) {
        double authority = nationAuthority.getOrDefault(nation.getUUID(), 0.0);
        logger.fine("Nation " + nation.getName() + " has " + authority + " authority");
        return authority;
    }

    public void setAuthority(Nation nation, double amount) {
        // Ensure authority can't go below 0 or above MAX_AUTHORITY
        double newAmount = Math.min(MAX_AUTHORITY, Math.max(0, amount));
        double oldAmount = nationAuthority.getOrDefault(nation.getUUID(), 0.0);

        nationAuthority.put(nation.getUUID(), newAmount);
        storage.saveAuthority(nation.getUUID(), newAmount);

        logger.info("Nation " + nation.getName() + " authority set to " +
                newAmount + " (was " + oldAmount + ")");
    }

    public void addAuthority(Nation nation, double amount) {
        double current = getAuthority(nation);
        double newAmount = current + amount;

        logger.info("Adding " + amount + " authority to nation " +
                nation.getName() + " (new total: " + newAmount + ")");

        setAuthority(nation, newAmount);
    }

    public boolean removeAuthority(Nation nation, double amount) {
        double current = getAuthority(nation);
        if (current >= amount) {
            double newAmount = current - amount;

            logger.info("Removing " + amount + " authority from nation " +
                    nation.getName() + " (new total: " + newAmount + ")");

            setAuthority(nation, newAmount);
            return true;
        }

        logger.warning("Cannot remove " + amount + " authority from nation " +
                nation.getName() + " (only has " + current + ")");
        return false;
    }

    public double getMaxAuthority() {
        return MAX_AUTHORITY;
    }

    public double calculateDailyAuthorityGain(Nation nation) {
        int residents = nation.getNumResidents();
        double baseGain = plugin.getConfig().getDouble("authority.base_gain", 1.0);
        double maxGain = plugin.getConfig().getDouble("authority.max_daily_gain", 5.0);
        double minGain = plugin.getConfig().getDouble("authority.min_daily_gain", 1.0);

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
        GovernmentType govType = govManager.getGovernmentType(nation);
        double govModifier = govType.getAuthorityModifier();
        authorityGain *= govModifier;

        // Apply decadence modifiers
        if (plugin.getDecadenceManager() != null) {
            double authorityModifier = plugin.getDecadenceManager().getAuthorityModifier(nation);
            authorityGain *= authorityModifier;
        }

        // Apply policy modifiers
        if (plugin.getPolicyEffectsHandler() != null) {
            double policyModifier = plugin.getPolicyEffectsHandler().getAuthorityGainModifier(nation);
            authorityGain *= policyModifier;
        }

        double finalGain = Math.min(maxGain, Math.max(minGain, authorityGain));

        logger.fine("Nation " + nation.getName() + " daily authority gain calculation: " +
                "residents=" + residents + ", baseGain=" + baseGain +
                ", govMod=" + govModifier + ", finalGain=" + finalGain);

        return finalGain;
    }

    public void processNewDay() {
        logger.info("Processing daily authority gains for all nations");

        plugin.getTownyAPI().getNations().forEach(nation -> {
            try {
                double gain = calculateDailyAuthorityGain(nation);
                addAuthority(nation, gain);
                logger.info("Nation " + nation.getName() + " gained " +
                        String.format("%.2f", gain) + " authority");
            } catch (Exception e) {
                logger.severe("Error processing authority for nation " + nation.getName() + ": " + e.getMessage());
            }
        });
    }

    // Legacy method aliases for backwards compatibility
    public double getPoliticalPower(Nation nation) {
        return getAuthority(nation);
    }

    public void setPoliticalPower(Nation nation, double amount) {
        setAuthority(nation, amount);
    }

    public void addPoliticalPower(Nation nation, double amount) {
        addAuthority(nation, amount);
    }

    public boolean removePoliticalPower(Nation nation, double amount) {
        return removeAuthority(nation, amount);
    }

    public double calculateDailyPPGain(Nation nation) {
        return calculateDailyAuthorityGain(nation);
    }
}