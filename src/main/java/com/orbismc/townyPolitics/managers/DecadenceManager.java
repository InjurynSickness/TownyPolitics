package com.orbismc.townyPolitics.managers;

import com.palmergames.bukkit.towny.object.Nation;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.storage.IEntityStorage;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DecadenceManager implements Manager {

    private final TownyPolitics plugin;
    private final IEntityStorage<Nation> storage;
    private final GovernmentManager govManager;
    private final DelegateLogger logger;

    // Cache of nation UUIDs to their decadence levels
    private final Map<UUID, Double> nationDecadence;

    // Maximum decadence level (100% is completely decadent)
    private final double MAX_DECADENCE = 100.0;

    // Decadence thresholds
    private final double LOW_THRESHOLD;
    private final double MEDIUM_THRESHOLD;
    private final double HIGH_THRESHOLD;
    private final double CRITICAL_THRESHOLD;

    public DecadenceManager(TownyPolitics plugin, IEntityStorage<Nation> storage, GovernmentManager govManager) {
        this.plugin = plugin;
        this.storage = storage;
        this.govManager = govManager;
        this.nationDecadence = new HashMap<>();
        this.logger = new DelegateLogger(plugin, "DecadenceManager");

        // Load configuration - threshold values
        this.LOW_THRESHOLD = plugin.getConfig().getDouble("decadence.thresholds.low", 25.0);
        this.MEDIUM_THRESHOLD = plugin.getConfig().getDouble("decadence.thresholds.medium", 50.0);
        this.HIGH_THRESHOLD = plugin.getConfig().getDouble("decadence.thresholds.high", 75.0);
        this.CRITICAL_THRESHOLD = plugin.getConfig().getDouble("decadence.thresholds.critical", 90.0);

        // Load data from storage
        loadData();
    }

    @Override
    public void loadData() {
        nationDecadence.clear();
        nationDecadence.putAll(storage.loadAllDecadence());
        logger.info("Loaded decadence data for " + nationDecadence.size() + " nations");
    }

    @Override
    public void saveAllData() {
        storage.saveAll();
        logger.info("Saved decadence data to storage");
    }

    public double getDecadence(Nation nation) {
        return nationDecadence.getOrDefault(nation.getUUID(), 0.0);
    }

    public void setDecadence(Nation nation, double amount) {
        // Ensure decadence can't go below 0 or above MAX_DECADENCE
        double newAmount = Math.min(MAX_DECADENCE, Math.max(0, amount));
        nationDecadence.put(nation.getUUID(), newAmount);
        storage.saveDecadence(nation.getUUID(), newAmount);
        logger.info("Set decadence for nation " + nation.getName() + " to " + newAmount);
    }

    public void addDecadence(Nation nation, double amount) {
        double current = getDecadence(nation);
        double newAmount = current + amount;
        setDecadence(nation, newAmount);
        logger.info("Added " + amount + " decadence to nation " + nation.getName() +
                " (now " + newAmount + "%)");
    }

    public void reduceDecadence(Nation nation, double amount) {
        double current = getDecadence(nation);
        double newAmount = current - amount;
        setDecadence(nation, newAmount);
        logger.info("Reduced decadence for nation " + nation.getName() +
                " by " + amount + " (now " + newAmount + "%)");
    }

    /**
     * Get the decadence threshold level for a nation
     *
     * @param nation The nation
     * @return The threshold level (0-4, where 4 is critical)
     */
    public int getDecadenceThresholdLevel(Nation nation) {
        double decadence = getDecadence(nation);

        if (decadence >= CRITICAL_THRESHOLD) return 4; // Critical
        if (decadence >= HIGH_THRESHOLD) return 3;     // High
        if (decadence >= MEDIUM_THRESHOLD) return 2;   // Medium
        if (decadence >= LOW_THRESHOLD) return 1;      // Low
        return 0;                                      // Minimal
    }

    /**
     * Check if nation's decadence is at or above a specific threshold
     *
     * @param nation         The nation
     * @param thresholdLevel The threshold level to check (0-4)
     * @return True if decadence is at or above the threshold
     */
    public boolean isDecadenceAtOrAboveThreshold(Nation nation, int thresholdLevel) {
        return getDecadenceThresholdLevel(nation) >= thresholdLevel;
    }

    /**
     * Check if a nation's decadence is at critical level
     *
     * @param nation The nation
     * @return True if decadence is critical
     */
    public boolean isDecadenceCritical(Nation nation) {
        return getDecadenceThresholdLevel(nation) == 4;
    }

    /**
     * Get the name of the decadence threshold for display
     *
     * @param thresholdLevel The threshold level (0-4)
     * @return The name of the threshold
     */
    public String getDecadenceThresholdName(int thresholdLevel) {
        return switch (thresholdLevel) {
            case 0 -> "Minimal";
            case 1 -> "Low";
            case 2 -> "Medium";
            case 3 -> "High";
            case 4 -> "Critical";
            default -> "Unknown";
        };
    }

    /**
     * Get the modifier for taxation based on decadence level
     * Higher decadence = higher potential maximum taxation
     *
     * @param nation The nation
     * @return The taxation modifier (0.0 to 1.0 representing 0-100%)
     */
    public double getTaxationModifier(Nation nation) {
        int thresholdLevel = getDecadenceThresholdLevel(nation);

        // Increasing effects based on threshold
        return switch (thresholdLevel) {
            case 0 -> 1.0;             // No effect
            case 1 -> 1.05;            // +5% max taxation
            case 2 -> 1.10;            // +10% max taxation
            case 3 -> 1.15;            // +15% max taxation
            case 4 -> 1.20;            // +20% max taxation
            default -> 1.0;
        };
    }

    /**
     * Get the modifier for authority gain based on decadence level
     * Higher decadence = lower authority gain
     *
     * @param nation The nation
     * @return The authority gain modifier (0.0 to 1.0)
     */
    public double getAuthorityModifier(Nation nation) {
        int thresholdLevel = getDecadenceThresholdLevel(nation);

        // Only medium+ decadence affects authority gain
        return switch (thresholdLevel) {
            case 0, 1 -> 1.0;          // No effect for minimal/low
            case 2 -> 0.90;            // -10% authority gain
            case 3 -> 0.75;            // -25% authority gain
            case 4 -> 0.50;            // -50% authority gain
            default -> 1.0;
        };
    }

    /**
     * Get the modifier for resource output based on decadence level
     * Higher decadence = lower resource output
     *
     * @param nation The nation
     * @return The resource modifier (0.0 to 1.0)
     */
    public double getResourceModifier(Nation nation) {
        int thresholdLevel = getDecadenceThresholdLevel(nation);

        // Decreasing output at each threshold
        return switch (thresholdLevel) {
            case 0 -> 1.0;             // No effect
            case 1 -> 0.95;            // -5% resource output
            case 2 -> 0.85;            // -15% resource output
            case 3 -> 0.75;            // -25% resource output
            case 4 -> 0.60;            // -40% resource output
            default -> 1.0;
        };
    }

    /**
     * Get the modifier for spending based on decadence level
     * Higher decadence = higher spending
     *
     * @param nation The nation
     * @return The spending modifier (1.0 or higher)
     */
    public double getSpendingModifier(Nation nation) {
        int thresholdLevel = getDecadenceThresholdLevel(nation);

        // Increasing costs at each threshold
        return switch (thresholdLevel) {
            case 0 -> 1.0;             // No effect
            case 1 -> 1.10;            // +10% spending
            case 2 -> 1.20;            // +20% spending
            case 3 -> 1.30;            // +30% spending
            case 4 -> 1.50;            // +50% spending
            default -> 1.0;
        };
    }

    /**
     * Calculate daily decadence gain for a nation based on its government type and other factors.
     *
     * @param nation The nation
     * @return The daily decadence gain
     */
    public double calculateDailyDecadenceGain(Nation nation) {
        // Base daily decadence gain from config
        double baseGain = plugin.getConfig().getDouble("decadence.base_daily_gain", 0.5);

        // Government type modifier
        GovernmentType govType = govManager.getGovernmentType(nation);
        double govModifier = govType.getDecadenceModifier();

        // Apply policy modifiers
        double policyModifier = 1.0;
        if (plugin.getPolicyEffectsHandler() != null) {
            policyModifier = plugin.getPolicyEffectsHandler().getDecadenceGainModifier(nation);
        }

        // Calculate final gain (minimum 0)
        double finalGain = Math.max(0, baseGain * govModifier * policyModifier);

        logger.fine("Daily decadence gain for " + nation.getName() +
                ": base=" + baseGain +
                ", govMod=" + govModifier +
                ", policyMod=" + policyModifier +
                ", final=" + finalGain);

        return finalGain;
    }

    /**
     * Process daily decadence changes for all nations
     */
    public void processNewDay() {
        logger.info("Processing daily decadence changes for all nations");

        plugin.getTownyAPI().getNations().forEach(nation -> {
            try {
                double gain = calculateDailyDecadenceGain(nation);

                // Store old threshold level
                int oldThresholdLevel = getDecadenceThresholdLevel(nation);

                // Add decadence
                addDecadence(nation, gain);

                // Get new threshold level
                int newThresholdLevel = getDecadenceThresholdLevel(nation);
                double currentDecadence = getDecadence(nation);

                logger.info("Nation " + nation.getName() + " gained " +
                        String.format("%.2f", gain) + " decadence, now at " +
                        String.format("%.2f", currentDecadence) + "% (" +
                        getDecadenceThresholdName(newThresholdLevel) + ")");

                // Apply threshold-based effects
                applyThresholdEffects(nation, oldThresholdLevel, newThresholdLevel);
            } catch (Exception e) {
                logger.severe("Error processing decadence for nation " + nation.getName() + ": " + e.getMessage());
            }
        });
    }

    /**
     * Apply effects based on decadence threshold level
     *
     * @param nation   The nation
     * @param oldLevel Previous decadence threshold level
     * @param newLevel Current decadence threshold level
     */
    private void applyThresholdEffects(Nation nation, int oldLevel, int newLevel) {
        // If threshold level increased, notify and apply immediate effects
        if (newLevel > oldLevel) {
            String newLevelName = getDecadenceThresholdName(newLevel);

            // Log the threshold change
            logger.info("Nation " + nation.getName() +
                    " decadence increased to " + newLevelName + " level!");

            // Apply effects based on new threshold level
            applyDecadenceEffects(nation, newLevel);
        }
        // Always apply daily effects for high/critical decadence
        else if (newLevel >= 3) {
            applyDecadenceEffects(nation, newLevel);
        }
    }

    /**
     * Apply negative effects based on decadence threshold level
     *
     * @param nation         The nation
     * @param thresholdLevel Current decadence threshold level (0-4)
     */
    private void applyDecadenceEffects(Nation nation, int thresholdLevel) {
        AuthorityManager authorityManager = plugin.getAuthorityManager();
        if (authorityManager == null) return;
        
        double currentAuthority = authorityManager.getAuthority(nation);
        double reduction = 0;

        // Apply effects based on threshold level
        switch (thresholdLevel) {
            case 3: // High decadence
                // 2.5% authority reduction
                reduction = currentAuthority * 0.025;
                break;

            case 4: // Critical decadence
                // 5% authority reduction
                reduction = currentAuthority * 0.05;

                // Additional critical effects could go here
                // - Random event chance
                // - Possible revolts
                // - Blocked certain actions
                break;
        }

        // Apply authority reduction if any
        if (reduction > 0) {
            authorityManager.removeAuthority(nation, reduction);
            logger.info("Nation " + nation.getName() +
                    " lost " + String.format("%.2f", reduction) +
                    " authority due to " + getDecadenceThresholdName(thresholdLevel) +
                    " decadence levels");
        }
    }

    /**
     * Calculate the authority cost to reduce decadence
     *
     * @param amount The amount of decadence to reduce
     * @return The authority cost
     */
    public double calculateAuthorityCostForDecadenceReduction(double amount) {
        // Base cost from config
        double baseRate = plugin.getConfig().getDouble("decadence.authority_cost_rate", 2.0);

        // Each point of decadence reduction costs baseRate authority
        return amount * baseRate;
    }

    // Legacy method aliases for backwards compatibility
    public double getCorruption(Nation nation) {
        return getDecadence(nation);
    }

    public void setCorruption(Nation nation, double amount) {
        setDecadence(nation, amount);
    }

    public void addCorruption(Nation nation, double amount) {
        addDecadence(nation, amount);
    }

    public void reduceCorruption(Nation nation, double amount) {
        reduceDecadence(nation, amount);
    }

    public double calculateDailyCorruptionGain(Nation nation) {
        return calculateDailyDecadenceGain(nation);
    }

    public int getCorruptionThresholdLevel(Nation nation) {
        return getDecadenceThresholdLevel(nation);
    }

    public boolean isCorruptionCritical(Nation nation) {
        return isDecadenceCritical(nation);
    }

    public String getCorruptionThresholdName(int thresholdLevel) {
        return getDecadenceThresholdName(thresholdLevel);
    }
}