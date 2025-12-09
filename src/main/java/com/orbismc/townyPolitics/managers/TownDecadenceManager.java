package com.orbismc.townyPolitics.managers;

import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.storage.IEntityStorage;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TownDecadenceManager implements Manager {

    private final TownyPolitics plugin;
    private final IEntityStorage<Town> storage;
    private final TownGovernmentManager townGovManager;
    private final DelegateLogger logger;

    // Cache of town UUIDs to their decadence levels
    private final Map<UUID, Double> townDecadence;

    // Maximum decadence level (100% is completely decadent)
    private final double MAX_DECADENCE = 100.0;

    // Decadence thresholds
    private final double LOW_THRESHOLD;
    private final double MEDIUM_THRESHOLD;
    private final double HIGH_THRESHOLD;
    private final double CRITICAL_THRESHOLD;

    public TownDecadenceManager(TownyPolitics plugin, IEntityStorage<Town> storage, TownGovernmentManager townGovManager) {
        this.plugin = plugin;
        this.storage = storage;
        this.townGovManager = townGovManager;
        this.townDecadence = new HashMap<>();
        this.logger = new DelegateLogger(plugin, "TownDecadence");

        // Load configuration - threshold values
        this.LOW_THRESHOLD = plugin.getConfig().getDouble("town_decadence.thresholds.low", 25.0);
        this.MEDIUM_THRESHOLD = plugin.getConfig().getDouble("town_decadence.thresholds.medium", 50.0);
        this.HIGH_THRESHOLD = plugin.getConfig().getDouble("town_decadence.thresholds.high", 75.0);
        this.CRITICAL_THRESHOLD = plugin.getConfig().getDouble("town_decadence.thresholds.critical", 90.0);

        // Load data from storage
        loadData();
    }

    @Override
    public void loadData() {
        townDecadence.clear();
        townDecadence.putAll(storage.loadAllDecadence());
        logger.info("Loaded decadence data for " + townDecadence.size() + " towns");
    }

    @Override
    public void saveAllData() {
        storage.saveAll();
        logger.info("Saved town decadence data to storage");
    }

    public double getDecadence(Town town) {
        return townDecadence.getOrDefault(town.getUUID(), 0.0);
    }

    public void setDecadence(Town town, double amount) {
        // Ensure decadence can't go below 0 or above MAX_DECADENCE
        double newAmount = Math.min(MAX_DECADENCE, Math.max(0, amount));
        townDecadence.put(town.getUUID(), newAmount);
        storage.saveDecadence(town.getUUID(), newAmount);
        logger.info("Set decadence for town " + town.getName() + " to " + newAmount);
    }

    public void addDecadence(Town town, double amount) {
        double current = getDecadence(town);
        double newAmount = current + amount;
        setDecadence(town, newAmount);
        logger.info("Added " + amount + " decadence to town " + town.getName() +
                " (now " + newAmount + "%)");
    }

    public void reduceDecadence(Town town, double amount) {
        double current = getDecadence(town);
        double newAmount = current - amount;
        setDecadence(town, newAmount);
        logger.info("Reduced decadence for town " + town.getName() +
                " by " + amount + " (now " + newAmount + "%)");
    }

    /**
     * Get the decadence threshold level for a town
     *
     * @param town The town
     * @return The threshold level (0-4, where 4 is critical)
     */
    public int getDecadenceThresholdLevel(Town town) {
        double decadence = getDecadence(town);

        if (decadence >= CRITICAL_THRESHOLD) return 4; // Critical
        if (decadence >= HIGH_THRESHOLD) return 3;     // High
        if (decadence >= MEDIUM_THRESHOLD) return 2;   // Medium
        if (decadence >= LOW_THRESHOLD) return 1;      // Low
        return 0;                                      // Minimal
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
     * Calculate daily decadence gain for a town based on its government type
     *
     * @param town The town
     * @return The daily decadence gain
     */
    public double calculateDailyDecadenceGain(Town town) {
        // Base daily decadence gain from config
        double baseGain = plugin.getConfig().getDouble("town_decadence.base_daily_gain", 0.4);

        // Government type modifier
        GovernmentType govType = townGovManager.getGovernmentType(town);
        double govModifier = govType.getDecadenceModifier();

        // Apply policy modifiers
        double policyModifier = 1.0;
        if (plugin.getPolicyEffectsHandler() != null) {
            policyModifier = plugin.getPolicyEffectsHandler().getDecadenceGainModifier(town);
        }

        // Calculate final gain (minimum 0)
        double finalGain = Math.max(0, baseGain * govModifier * policyModifier);

        logger.fine("Daily decadence gain for " + town.getName() +
                ": base=" + baseGain +
                ", govMod=" + govModifier +
                ", policyMod=" + policyModifier +
                ", final=" + finalGain);

        return finalGain;
    }

    /**
     * Process daily decadence changes for all towns
     */
    public void processNewDay() {
        logger.info("Processing daily decadence changes for all towns");

        plugin.getTownyAPI().getTowns().forEach(town -> {
            try {
                double gain = calculateDailyDecadenceGain(town);

                // Store old threshold level
                int oldThresholdLevel = getDecadenceThresholdLevel(town);

                // Add decadence
                addDecadence(town, gain);

                // Get new threshold level
                int newThresholdLevel = getDecadenceThresholdLevel(town);
                double currentDecadence = getDecadence(town);

                logger.info("Town " + town.getName() + " gained " +
                        String.format("%.2f", gain) + " decadence, now at " +
                        String.format("%.2f", currentDecadence) + "% (" +
                        getDecadenceThresholdName(newThresholdLevel) + ")");

                // Apply threshold-based effects
                applyThresholdEffects(town, oldThresholdLevel, newThresholdLevel);
            } catch (Exception e) {
                logger.severe("Error processing decadence for town " + town.getName() + ": " + e.getMessage());
            }
        });
    }

    /**
     * Apply effects based on decadence threshold level
     *
     * @param town The town
     * @param oldLevel Previous decadence threshold level
     * @param newLevel Current decadence threshold level
     */
    private void applyThresholdEffects(Town town, int oldLevel, int newLevel) {
        // If threshold level increased, notify and apply immediate effects
        if (newLevel > oldLevel) {
            String newLevelName = getDecadenceThresholdName(newLevel);

            // Log the threshold change
            logger.info("Town " + town.getName() +
                    " decadence increased to " + newLevelName + " level!");

            // Apply effects based on new threshold level
            applyDecadenceEffects(town, newLevel);
        }
        // Always apply daily effects for high/critical decadence
        else if (newLevel >= 3) {
            applyDecadenceEffects(town, newLevel);
        }
    }

    /**
     * Apply negative effects based on decadence threshold level
     *
     * @param town The town
     * @param thresholdLevel Current decadence threshold level (0-4)
     */
    private void applyDecadenceEffects(Town town, int thresholdLevel) {
        // Town decadence doesn't affect authority or spending as specified
        // But we can add future effects here if needed

        switch (thresholdLevel) {
            case 3: // High decadence
                // Future high decadence effects could go here
                break;

            case 4: // Critical decadence
                // Future critical decadence effects could go here
                break;
        }
    }

    /**
     * Get the taxation modifier for towns based on decadence
     *
     * @param town The town
     * @return The taxation modifier (lower with higher decadence)
     */
    public double getTaxationModifier(Town town) {
        int thresholdLevel = getDecadenceThresholdLevel(town);

        // Get values from config or use defaults
        double lowMod = plugin.getConfig().getDouble("town_decadence.effects.taxation.low", 0.95);
        double mediumMod = plugin.getConfig().getDouble("town_decadence.effects.taxation.medium", 0.90);
        double highMod = plugin.getConfig().getDouble("town_decadence.effects.taxation.high", 0.80);
        double criticalMod = plugin.getConfig().getDouble("town_decadence.effects.taxation.critical", 0.70);

        // Return appropriate modifier based on threshold
        return switch (thresholdLevel) {
            case 0 -> 1.0;             // No effect
            case 1 -> lowMod;          // -5% tax income by default
            case 2 -> mediumMod;       // -10% tax income by default
            case 3 -> highMod;         // -20% tax income by default
            case 4 -> criticalMod;     // -30% tax income by default
            default -> 1.0;
        };
    }

    /**
     * Get the trade modifier for towns based on decadence
     *
     * @param town The town
     * @return The trade modifier (lower with higher decadence)
     */
    public double getTradeModifier(Town town) {
        int thresholdLevel = getDecadenceThresholdLevel(town);

        // Get values from config or use defaults
        double lowMod = plugin.getConfig().getDouble("town_decadence.effects.trade.low", 0.95);
        double mediumMod = plugin.getConfig().getDouble("town_decadence.effects.trade.medium", 0.90);
        double highMod = plugin.getConfig().getDouble("town_decadence.effects.trade.high", 0.80);
        double criticalMod = plugin.getConfig().getDouble("town_decadence.effects.trade.critical", 0.70);

        // Return appropriate modifier based on threshold
        return switch (thresholdLevel) {
            case 0 -> 1.0;             // No effect
            case 1 -> lowMod;          // -5% trade income by default
            case 2 -> mediumMod;       // -10% trade income by default
            case 3 -> highMod;         // -20% trade income by default
            case 4 -> criticalMod;     // -30% trade income by default
            default -> 1.0;
        };
    }

    // Legacy method aliases for backwards compatibility
    public double getCorruption(Town town) {
        return getDecadence(town);
    }

    public void setCorruption(Town town, double amount) {
        setDecadence(town, amount);
    }

    public void addCorruption(Town town, double amount) {
        addDecadence(town, amount);
    }

    public void reduceCorruption(Town town, double amount) {
        reduceDecadence(town, amount);
    }

    public double calculateDailyCorruptionGain(Town town) {
        return calculateDailyDecadenceGain(town);
    }

    public int getCorruptionThresholdLevel(Town town) {
        return getDecadenceThresholdLevel(town);
    }

    public String getCorruptionThresholdName(int thresholdLevel) {
        return getDecadenceThresholdName(thresholdLevel);
    }
}