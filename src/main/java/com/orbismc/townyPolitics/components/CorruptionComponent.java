package com.orbismc.townyPolitics.components;

import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.palmergames.adventure.text.Component;
import com.palmergames.adventure.text.format.NamedTextColor;
import com.palmergames.adventure.text.event.HoverEvent;

/**
 * Component that displays Decadence information on status screens
 */
public class CorruptionComponent extends StatusComponent {

    public CorruptionComponent(TownyPolitics plugin) {
        super(plugin, "Decadence");
    }

    @Override
    public void addToNationScreen(NationStatusScreenEvent event, Nation nation) {
        // Get decadence data
        double decadence = plugin.getDecadenceManager().getDecadence(nation);
        double dailyDecadenceGain = plugin.getDecadenceManager().calculateDailyDecadenceGain(nation);

        // Get modifiers
        double taxMod = plugin.getDecadenceManager().getTaxationModifier(nation);
        double ppMod = plugin.getDecadenceManager().getAuthorityModifier(nation);
        double resourceMod = plugin.getDecadenceManager().getResourceModifier(nation);
        double spendingMod = plugin.getDecadenceManager().getSpendingModifier(nation);

        // Format modifiers as percentages
        String taxModStr = String.format("%+.1f%%", (taxMod - 1.0) * 100);
        String ppModStr = String.format("%+.1f%%", (ppMod - 1.0) * 100);
        String resourceModStr = String.format("%+.1f%%", (resourceMod - 1.0) * 100);
        String spendingModStr = String.format("%+.1f%%", (spendingMod - 1.0) * 100);

        // Get decadence threshold level and determine color
        int thresholdLevel = plugin.getDecadenceManager().getDecadenceThresholdLevel(nation);
        NamedTextColor decadenceColor = getDecadenceColorForThreshold(thresholdLevel);

        // Get threshold name
        String thresholdName = plugin.getDecadenceManager().getDecadenceThresholdName(thresholdLevel);

        // Create hover text component
        Component hoverText = Component.text("Decadence Information")
                .color(NamedTextColor.DARK_GREEN)
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Status: ")
                        .color(NamedTextColor.YELLOW))
                .append(Component.text(thresholdName)
                        .color(decadenceColor))
                .append(Component.newline())
                .append(Component.text("Current Level: " + String.format("%.1f%%", decadence))
                        .color(decadenceColor))
                .append(Component.newline())
                .append(Component.text("Daily Change: +" + String.format("%.2f%%", dailyDecadenceGain))
                        .color(NamedTextColor.RED))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Effects:")
                        .color(NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("• Tax Collection: " + String.format("%+.1f%%", -decadence * 5))
                        .color(NamedTextColor.RED))
                .append(Component.newline())
                .append(Component.text("• Authority Gain: " + ppModStr)
                        .color(getTextColorForValue(ppMod - 1.0, false)))
                .append(Component.newline())
                .append(Component.text("• Resource Output: " + resourceModStr)
                        .color(getTextColorForValue(resourceMod - 1.0, false)))
                .append(Component.newline())
                .append(Component.text("• Spending Costs: " + spendingModStr)
                        .color(getTextColorForValue(spendingMod - 1.0, true)));

        // Create the Decadence component
        Component openBracket = Component.text("[").color(NamedTextColor.GRAY);
        Component decadenceText = Component.text("Decadence").color(decadenceColor);
        Component closeBracket = Component.text("]").color(NamedTextColor.GRAY);

        Component decadenceComponent = Component.empty()
                .append(openBracket)
                .append(decadenceText)
                .append(closeBracket)
                .hoverEvent(HoverEvent.showText(hoverText));

        // Add to status screen
        addComponentToScreen(event, "decadence_display", decadenceComponent);
    }

    @Override
    public void addToTownScreen(TownStatusScreenEvent event, Town town) {
        // Get decadence data
        double decadence = plugin.getTownDecadenceManager().getDecadence(town);
        double dailyDecadenceGain = plugin.getTownDecadenceManager().calculateDailyDecadenceGain(town);

        // Get modifiers
        double taxMod = plugin.getTownDecadenceManager().getTaxationModifier(town);
        double tradeMod = plugin.getTownDecadenceManager().getTradeModifier(town);

        // Format modifiers as percentages
        String taxModStr = String.format("%+.1f%%", (taxMod - 1.0) * 100);
        String tradeModStr = String.format("%+.1f%%", (tradeMod - 1.0) * 100);

        // Get decadence threshold level and determine color
        int thresholdLevel = plugin.getTownDecadenceManager().getDecadenceThresholdLevel(town);
        NamedTextColor decadenceColor = getDecadenceColorForThreshold(thresholdLevel);

        // Get threshold name
        String thresholdName = plugin.getTownDecadenceManager().getDecadenceThresholdName(thresholdLevel);

        // Create hover text component
        Component hoverText = Component.text("Town Decadence Information")
                .color(NamedTextColor.DARK_GREEN)
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Status: ")
                        .color(NamedTextColor.YELLOW))
                .append(Component.text(thresholdName)
                        .color(decadenceColor))
                .append(Component.newline())
                .append(Component.text("Current Level: " + String.format("%.1f%%", decadence))
                        .color(decadenceColor))
                .append(Component.newline())
                .append(Component.text("Daily Change: +" + String.format("%.2f%%", dailyDecadenceGain))
                        .color(NamedTextColor.RED))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Effects:")
                        .color(NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("• Tax Income: " + taxModStr)
                        .color(getTextColorForValue(taxMod - 1.0, false)))
                .append(Component.newline())
                .append(Component.text("• Trade Income: " + tradeModStr)
                        .color(getTextColorForValue(tradeMod - 1.0, false)));

        // Create the Decadence component
        Component openBracket = Component.text("[").color(NamedTextColor.GRAY);
        Component decadenceText = Component.text("Decadence").color(decadenceColor);
        Component closeBracket = Component.text("]").color(NamedTextColor.GRAY);

        Component decadenceComponent = Component.empty()
                .append(openBracket)
                .append(decadenceText)
                .append(closeBracket)
                .hoverEvent(HoverEvent.showText(hoverText));

        // Add to status screen
        addComponentToScreen(event, "town_decadence_display", decadenceComponent);
    }

    /**
     * Get the appropriate color for a decadence threshold level
     */
    private NamedTextColor getDecadenceColorForThreshold(int thresholdLevel) {
        return switch (thresholdLevel) {
            case 0 -> NamedTextColor.GREEN;       // Minimal
            case 1 -> NamedTextColor.YELLOW;      // Low
            case 2 -> NamedTextColor.GOLD;        // Medium
            case 3 -> NamedTextColor.RED;         // High
            case 4 -> NamedTextColor.DARK_RED;    // Critical
            default -> NamedTextColor.GREEN;
        };
    }
}