package com.orbismc.townyPolitics.policy;

public class PolicyEffects {
    // Economic effects
    private final double taxModifier;
    private final double tradeModifier;
    private final double economyModifier;

    // Political effects
    private final double authorityGainModifier;
    private final double decadenceGainModifier;
    private final double maxAuthorityModifier;

    // Resource effects
    private final double resourceOutputModifier;
    private final double spendingModifier;

    // Town-specific effects
    private final double plotCostModifier;
    private final double plotTaxModifier;
    private final double residentCapacityModifier;
    private final double upkeepModifier;
    private final double townBlockCostModifier;
    private final double townBlockBonusModifier;

    public PolicyEffects(double taxModifier, double tradeModifier, double economyModifier,
                         double authorityGainModifier, double decadenceGainModifier,
                         double maxAuthorityModifier,
                         double resourceOutputModifier, double spendingModifier,
                         double plotCostModifier, double plotTaxModifier, double residentCapacityModifier,
                         double upkeepModifier, double townBlockCostModifier, double townBlockBonusModifier) {
        this.taxModifier = taxModifier;
        this.tradeModifier = tradeModifier;
        this.economyModifier = economyModifier;
        this.authorityGainModifier = authorityGainModifier;
        this.decadenceGainModifier = decadenceGainModifier;
        this.maxAuthorityModifier = maxAuthorityModifier;
        this.resourceOutputModifier = resourceOutputModifier;
        this.spendingModifier = spendingModifier;
        this.plotCostModifier = plotCostModifier;
        this.plotTaxModifier = plotTaxModifier;
        this.residentCapacityModifier = residentCapacityModifier;
        this.upkeepModifier = upkeepModifier;
        this.townBlockCostModifier = townBlockCostModifier;
        this.townBlockBonusModifier = townBlockBonusModifier;
    }

    // Getters for existing effects
    public double getTaxModifier() { return taxModifier; }
    public double getTradeModifier() { return tradeModifier; }
    public double getEconomyModifier() { return economyModifier; }
    public double getAuthorityGainModifier() { return authorityGainModifier; }
    public double getDecadenceGainModifier() { return decadenceGainModifier; }
    public double getMaxAuthorityModifier() { return maxAuthorityModifier; }
    public double getResourceOutputModifier() { return resourceOutputModifier; }
    public double getSpendingModifier() { return spendingModifier; }

    // Getters for town-specific effects
    public double getPlotCostModifier() { return plotCostModifier; }
    public double getPlotTaxModifier() { return plotTaxModifier; }
    public double getResidentCapacityModifier() { return residentCapacityModifier; }
    public double getUpkeepModifier() { return upkeepModifier; }
    public double getTownBlockCostModifier() { return townBlockCostModifier; }
    public double getTownBlockBonusModifier() { return townBlockBonusModifier; }

    public boolean hasTownEffects() {
        return plotCostModifier != 1.0 ||
                plotTaxModifier != 1.0 ||
                residentCapacityModifier != 1.0 ||
                upkeepModifier != 1.0 ||
                townBlockCostModifier != 1.0 ||
                townBlockBonusModifier != 1.0;
    }

    public static class Builder {
        // Default values (1.0 = no effect)
        private double taxModifier = 1.0;
        private double tradeModifier = 1.0;
        private double economyModifier = 1.0;
        private double authorityGainModifier = 1.0;
        private double decadenceGainModifier = 1.0;
        private double maxAuthorityModifier = 1.0;
        private double resourceOutputModifier = 1.0;
        private double spendingModifier = 1.0;
        private double plotCostModifier = 1.0;
        private double plotTaxModifier = 1.0;
        private double residentCapacityModifier = 1.0;
        private double upkeepModifier = 1.0;
        private double townBlockCostModifier = 1.0;
        private double townBlockBonusModifier = 1.0;

        public Builder taxModifier(double taxModifier) {
            this.taxModifier = taxModifier;
            return this;
        }

        public Builder tradeModifier(double tradeModifier) {
            this.tradeModifier = tradeModifier;
            return this;
        }

        public Builder economyModifier(double economyModifier) {
            this.economyModifier = economyModifier;
            return this;
        }

        public Builder authorityGainModifier(double authorityGainModifier) {
            this.authorityGainModifier = authorityGainModifier;
            return this;
        }

        public Builder decadenceGainModifier(double decadenceGainModifier) {
            this.decadenceGainModifier = decadenceGainModifier;
            return this;
        }

        public Builder maxAuthorityModifier(double maxAuthorityModifier) {
            this.maxAuthorityModifier = maxAuthorityModifier;
            return this;
        }

        public Builder resourceOutputModifier(double resourceOutputModifier) {
            this.resourceOutputModifier = resourceOutputModifier;
            return this;
        }

        public Builder spendingModifier(double spendingModifier) {
            this.spendingModifier = spendingModifier;
            return this;
        }

        public Builder plotCostModifier(double plotCostModifier) {
            this.plotCostModifier = plotCostModifier;
            return this;
        }

        public Builder plotTaxModifier(double plotTaxModifier) {
            this.plotTaxModifier = plotTaxModifier;
            return this;
        }

        public Builder residentCapacityModifier(double residentCapacityModifier) {
            this.residentCapacityModifier = residentCapacityModifier;
            return this;
        }

        public Builder upkeepModifier(double upkeepModifier) {
            this.upkeepModifier = upkeepModifier;
            return this;
        }

        public Builder townBlockCostModifier(double townBlockCostModifier) {
            this.townBlockCostModifier = townBlockCostModifier;
            return this;
        }

        public Builder townBlockBonusModifier(double townBlockBonusModifier) {
            this.townBlockBonusModifier = townBlockBonusModifier;
            return this;
        }

        public PolicyEffects build() {
            return new PolicyEffects(
                    taxModifier, tradeModifier, economyModifier,
                    authorityGainModifier, decadenceGainModifier, maxAuthorityModifier,
                    resourceOutputModifier, spendingModifier,
                    plotCostModifier, plotTaxModifier, residentCapacityModifier,
                    upkeepModifier, townBlockCostModifier, townBlockBonusModifier
            );
        }
    }
}