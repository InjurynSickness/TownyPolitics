package com.orbismc.townyPolitics.policy;

import com.orbismc.townyPolitics.government.GovernmentType;
import java.util.HashSet;
import java.util.Set;

public class Policy {
    private final String id;
    private final String name;
    private final String description;
    private final double cost;
    private final int duration; // -1 for permanent
    private final PolicyType type;
    private final Set<GovernmentType> allowedGovernments;
    private final double minPoliticalPower;
    private final double maxCorruption;
    private final boolean townOnly;
    private final PolicyEffects effects;

    public Policy(String id, String name, String description, double cost, int duration,
                  PolicyType type, Set<GovernmentType> allowedGovernments,
                  double minPoliticalPower, double maxCorruption,
                  boolean townOnly, PolicyEffects effects) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.cost = cost;
        this.duration = duration;
        this.type = type;
        this.allowedGovernments = allowedGovernments != null ? allowedGovernments : new HashSet<>();
        this.minPoliticalPower = minPoliticalPower;
        this.maxCorruption = maxCorruption;
        this.townOnly = townOnly;
        this.effects = effects;
    }

    // Constructor for backward compatibility
    public Policy(String id, String name, String description, double cost, int duration,
                  PolicyType type, Set<GovernmentType> allowedGovernments,
                  double minPoliticalPower, double maxCorruption, PolicyEffects effects) {
        this(id, name, description, cost, duration, type, allowedGovernments,
                minPoliticalPower, maxCorruption, false, effects);
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getCost() { return cost; }
    public int getDuration() { return duration; }
    public PolicyType getType() { return type; }
    public Set<GovernmentType> getAllowedGovernments() { return allowedGovernments; }
    public double getMinPoliticalPower() { return minPoliticalPower; }
    public double getMaxCorruption() { return maxCorruption; }
    public boolean isTownOnly() { return townOnly; }
    public PolicyEffects getEffects() { return effects; }

    public boolean isGovernmentAllowed(GovernmentType type) {
        return allowedGovernments.isEmpty() || allowedGovernments.contains(type);
    }

    public String getEnhancedDescription() {
        StringBuilder description = new StringBuilder(this.description);

        if (this.effects.hasTownEffects()) {
            description.append("\n\nTown Effects:");
            if (this.effects.getPlotCostModifier() != 1.0) {
                description.append("\n• Plot Cost: ").append(formatModifier(this.effects.getPlotCostModifier()));
            }
            if (this.effects.getPlotTaxModifier() != 1.0) {
                description.append("\n• Plot Tax: ").append(formatModifier(this.effects.getPlotTaxModifier()));
            }
            if (this.effects.getResidentCapacityModifier() != 1.0) {
                description.append("\n• Resident Capacity: ").append(formatModifier(this.effects.getResidentCapacityModifier()));
            }
            if (this.effects.getUpkeepModifier() != 1.0) {
                description.append("\n• Town Upkeep: ").append(formatModifier(this.effects.getUpkeepModifier()));
            }
            if (this.effects.getTownBlockCostModifier() != 1.0) {
                description.append("\n• Town Block Cost: ").append(formatModifier(this.effects.getTownBlockCostModifier()));
            }
            if (this.effects.getTownBlockBonusModifier() != 1.0) {
                description.append("\n• Town Block Bonus: ").append(formatModifier(this.effects.getTownBlockBonusModifier()));
            }
        }

        return description.toString();
    }

    private String formatModifier(double value) {
        return String.format("%+.1f%%", (value - 1.0) * 100);
    }

    public enum PolicyType {
        ECONOMIC,
        POLITICAL,
        MILITARY,
        SOCIAL,
        URBAN // Specifically for town development policies
    }
}