package com.orbismc.townyPolitics.handlers;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.policy.PolicyEffects;
import com.orbismc.townyPolitics.utils.DelegateLogger;

/**
 * Centralizes application of policy effects to gameplay mechanics
 */
public class PolicyEffectsHandler {
    private final TownyPolitics plugin;
    private final DelegateLogger logger;

    public PolicyEffectsHandler(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "PolicyEffects");
        logger.info("Policy Effects Handler initialized");
    }

    /**
     * Get the tax modifier for a town considering all active policies
     */
    public double getTaxModifier(Town town) {
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(town);
        double modifier = effects.getTaxModifier();
        logger.fine("Town " + town.getName() + " tax modifier from policies: " + modifier);
        return modifier;
    }

    /**
     * Get the trade modifier for a town considering all active policies
     */
    public double getTradeModifier(Town town) {
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(town);
        double modifier = effects.getTradeModifier();
        logger.fine("Town " + town.getName() + " trade modifier from policies: " + modifier);
        return modifier;
    }

    /**
     * Get the town block cost modifier for a town considering all active policies
     */
    public double getTownBlockCostModifier(Town town) {
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(town);
        double modifier = effects.getTownBlockCostModifier();
        logger.fine("Town " + town.getName() + " town block cost modifier from policies: " + modifier);
        return modifier;
    }

    /**
     * Get the plot cost modifier for a town considering all active policies
     */
    public double getPlotCostModifier(Town town) {
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(town);
        double modifier = effects.getPlotCostModifier();
        logger.fine("Town " + town.getName() + " plot cost modifier from policies: " + modifier);
        return modifier;
    }

    /**
     * Get the upkeep modifier for a town considering all active policies
     */
    public double getUpkeepModifier(Town town) {
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(town);
        double modifier = effects.getUpkeepModifier();
        logger.fine("Town " + town.getName() + " upkeep modifier from policies: " + modifier);
        return modifier;
    }

    /**
     * Get the decadence gain modifier for a town considering all active policies
     */
    public double getDecadenceGainModifier(Town town) {
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(town);
        double modifier = effects.getDecadenceGainModifier();
        logger.fine("Town " + town.getName() + " decadence gain modifier from policies: " + modifier);
        return modifier;
    }

    /**
     * Get the authority gain modifier for a town considering all active policies
     */
    public double getAuthorityGainModifier(Town town) {
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(town);
        double modifier = effects.getAuthorityGainModifier();
        logger.fine("Town " + town.getName() + " authority gain modifier from policies: " + modifier);
        return modifier;
    }

    /**
     * Get the tax modifier for a nation considering all active policies
     */
    public double getTaxModifier(Nation nation) {
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(nation);
        double modifier = effects.getTaxModifier();
        logger.fine("Nation " + nation.getName() + " tax modifier from policies: " + modifier);
        return modifier;
    }

    /**
     * Get the decadence gain modifier for a nation considering all active policies
     */
    public double getDecadenceGainModifier(Nation nation) {
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(nation);
        double modifier = effects.getDecadenceGainModifier();
        logger.fine("Nation " + nation.getName() + " decadence gain modifier from policies: " + modifier);
        return modifier;
    }

    /**
     * Get the authority gain modifier for a nation considering all active policies
     */
    public double getAuthorityGainModifier(Nation nation) {
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(nation);
        double modifier = effects.getAuthorityGainModifier();
        logger.fine("Nation " + nation.getName() + " authority gain modifier from policies: " + modifier);
        return modifier;
    }
}