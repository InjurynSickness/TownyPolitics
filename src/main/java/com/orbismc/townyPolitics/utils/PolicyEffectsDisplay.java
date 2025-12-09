package com.orbismc.townyPolitics.utils;

import com.orbismc.townyPolitics.policy.PolicyEffects;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PolicyEffectsDisplay {

    public static void displayEffects(Player player, PolicyEffects effects) {
        displayModifier(player, "Tax Rate", effects.getTaxModifier());
        displayModifier(player, "Trade Income", effects.getTradeModifier());
        displayModifier(player, "Economy", effects.getEconomyModifier());
        displayModifier(player, "Political Power Gain", effects.getAuthorityGainModifier());
        displayModifier(player, "Corruption Gain", effects.getDecadenceGainModifier(), true);
        displayModifier(player, "Max Political Power", effects.getMaxAuthorityModifier());
        displayModifier(player, "Resource Output", effects.getResourceOutputModifier());
        displayModifier(player, "Spending Costs", effects.getSpendingModifier(), true);

        if (effects.hasTownEffects()) {
            player.sendMessage(ChatColor.GOLD + "Town-specific effects:");
            displayModifier(player, "Plot Cost", effects.getPlotCostModifier(), true);
            displayModifier(player, "Plot Tax", effects.getPlotTaxModifier(), true);
            displayModifier(player, "Resident Capacity", effects.getResidentCapacityModifier());
            displayModifier(player, "Town Upkeep", effects.getUpkeepModifier(), true);
            displayModifier(player, "Town Block Cost", effects.getTownBlockCostModifier(), true);
            displayModifier(player, "Town Block Bonus", effects.getTownBlockBonusModifier());
        }
    }

    public static void displayTownEffects(Player player, PolicyEffects effects) {
        if (effects.hasTownEffects()) {
            displayModifier(player, "Plot Cost", effects.getPlotCostModifier(), true);
            displayModifier(player, "Plot Tax", effects.getPlotTaxModifier(), true);
            displayModifier(player, "Resident Capacity", effects.getResidentCapacityModifier());
            displayModifier(player, "Town Upkeep", effects.getUpkeepModifier(), true);
            displayModifier(player, "Town Block Cost", effects.getTownBlockCostModifier(), true);
            displayModifier(player, "Town Block Bonus", effects.getTownBlockBonusModifier());
        } else {
            player.sendMessage(ChatColor.GRAY + "• " + ChatColor.WHITE + "No specific town effects");
        }

        displayModifier(player, "Tax Rate", effects.getTaxModifier());
        displayModifier(player, "Trade Income", effects.getTradeModifier());
        displayModifier(player, "Economy", effects.getEconomyModifier());
        displayModifier(player, "Corruption Gain", effects.getAuthorityGainModifier(), true);
    }

    private static void displayModifier(Player player, String label, double value) {
        displayModifier(player, label, value, false);
    }

    private static void displayModifier(Player player, String label, double value, boolean invert) {
        if (value == 1.0) return;  // No effect

        String formattedValue = formatModifier(value);
        ChatColor color;

        if (invert) {
            color = (value > 1.0) ? ChatColor.RED : ChatColor.GREEN;
        } else {
            color = (value > 1.0) ? ChatColor.GREEN : ChatColor.RED;
        }

        player.sendMessage(ChatColor.GRAY + "• " + ChatColor.WHITE + label + ": " + color + formattedValue);
    }

    public static String formatModifier(double value) {
        return String.format("%+.1f%%", (value - 1.0) * 100);
    }
}