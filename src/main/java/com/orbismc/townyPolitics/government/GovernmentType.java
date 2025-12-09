package com.orbismc.townyPolitics.government;

import java.util.Arrays;

public enum GovernmentType {
    TRIBAL("Tribal", "Tribal Leadership Effects:\n• Village Bonds: -20% Decadence gain\n• Simple Authority: +0.5 Authority gain per day", false, true), // Town only
    FEUDAL("Feudalism", "Feudal Effects:\n• Noble Hierarchy: +1.0 Authority gain per day\n• Court Intrigue: +10% Decadence gain", false, false),
    ABSOLUTE_MONARCHY("Absolute Monarchy", "Absolute Monarchy Effects:\n• Divine Right: +15% Authority gain\n• Royal Decree: -30% Policy costs\n• Courtly Excess: +15% Decadence gain", true, false), // Nation only
    THEOCRACY("Theocracy", "Theocracy Effects:\n• Divine Authority: +20% Authority gain\n• Sacred Rule: -25% Decadence gain\n• Religious Unity: +10% Policy effectiveness", true, false), // Nation only
    AUTOCRACY("Autocracy", "Autocracy Effects:\n• Strong Leadership: +10% Authority gain\n• Centralized Control: -5% Decadence gain\n• Limited Freedoms: +5% Upkeep costs", false, false); // Both

    private final String displayName;
    private final String description;
    private final boolean nationOnly;
    private final boolean townOnly;

    GovernmentType(String displayName, String description, boolean nationOnly, boolean townOnly) {
        this.displayName = displayName;
        this.description = description;
        this.nationOnly = nationOnly;
        this.townOnly = townOnly;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isNationOnly() {
        return nationOnly;
    }

    public boolean isTownOnly() {
        return townOnly;
    }

    public boolean isDemocracy() {
        // In medieval times, no democratic governments
        return false;
    }

    public static GovernmentType[] getTownGovernmentTypes() {
        return Arrays.stream(values())
                .filter(type -> !type.isNationOnly())
                .toArray(GovernmentType[]::new);
    }

    public static GovernmentType[] getNationGovernmentTypes() {
        return Arrays.stream(values())
                .filter(type -> !type.isTownOnly())
                .toArray(GovernmentType[]::new);
    }

    public static GovernmentType getByName(String name) {
        for (GovernmentType type : values()) {
            if (type.name().equalsIgnoreCase(name) || type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Get the default government type for new towns
     */
    public static GovernmentType getDefaultTownGovernment() {
        return TRIBAL;
    }

    /**
     * Get the default government type for new nations
     */
    public static GovernmentType getDefaultNationGovernment() {
        return AUTOCRACY;  // Changed from TRIBAL to AUTOCRACY
    }

    /**
     * Check if this government type is valid for towns
     */
    public boolean isValidForTowns() {
        return !nationOnly;
    }

    /**
     * Check if this government type is valid for nations
     */
    public boolean isValidForNations() {
        return !townOnly;
    }

    /**
     * Check if this government type provides authority bonuses
     */
    public boolean providesAuthorityBonus() {
        return this == FEUDAL || this == ABSOLUTE_MONARCHY || this == THEOCRACY || this == AUTOCRACY;
    }

    /**
     * Check if this government type provides decadence resistance
     */
    public boolean providesDecadenceResistance() {
        return this == TRIBAL || this == THEOCRACY || this == AUTOCRACY;
    }

    /**
     * Get the authority modifier for this government type
     */
    public double getAuthorityModifier() {
        return switch (this) {
            case TRIBAL -> 1.0; // Base authority gain
            case FEUDAL -> 1.2; // +20% authority gain
            case ABSOLUTE_MONARCHY -> 1.15; // +15% authority gain
            case THEOCRACY -> 1.20; // +20% authority gain
            case AUTOCRACY -> 1.10; // +10% authority gain
        };
    }

    /**
     * Get the decadence modifier for this government type
     */
    public double getDecadenceModifier() {
        return switch (this) {
            case TRIBAL -> 0.8; // -20% decadence gain
            case FEUDAL -> 1.1; // +10% decadence gain
            case ABSOLUTE_MONARCHY -> 1.15; // +15% decadence gain
            case THEOCRACY -> 0.75; // -25% decadence gain
            case AUTOCRACY -> 0.95; // -5% decadence gain
        };
    }

    /**
     * Get the policy cost modifier for this government type
     */
    public double getPolicyCostModifier() {
        return switch (this) {
            case ABSOLUTE_MONARCHY -> 0.7; // -30% policy costs
            case THEOCRACY -> 0.9; // -10% policy costs (from religious unity)
            default -> 1.0; // No modification
        };
    }
}