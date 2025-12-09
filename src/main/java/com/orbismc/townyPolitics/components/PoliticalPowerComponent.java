package com.orbismc.townyPolitics.components;

import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.metadata.StringDataField;
import com.orbismc.townyPolitics.TownyPolitics;
import com.palmergames.adventure.text.Component;
import com.palmergames.adventure.text.format.NamedTextColor;
import com.palmergames.adventure.text.event.HoverEvent;

/**
 * Component that displays Authority information on status screens
 */
public class PoliticalPowerComponent extends StatusComponent {

    private static final String STORAGE_KEY = "townypolitics_storage";
    private static final String POLITICAL_POWER_KEY = "political_power";

    public PoliticalPowerComponent(TownyPolitics plugin) {
        super(plugin, "Authority");
    }

    @Override
    public void addToNationScreen(NationStatusScreenEvent event, Nation nation) {
        double currentAuthority = plugin.getAuthorityManager().getAuthority(nation);
        double dailyGain = plugin.getAuthorityManager().calculateDailyAuthorityGain(nation);
        int residents = nation.getNumResidents();

        // Create hover text component
        Component hoverText = Component.text("Authority Details")
                .color(NamedTextColor.DARK_GREEN)
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Current Authority: " + String.format("%.2f", currentAuthority))
                        .color(NamedTextColor.GREEN))
                .append(Component.newline())
                .append(Component.text("Daily Gain: +" + String.format("%.2f", dailyGain) + "/day")
                        .color(NamedTextColor.GREEN))
                .append(Component.newline())
                .append(Component.text("Residents: " + residents)
                        .color(NamedTextColor.GREEN));

        // Create the Authority component
        Component openBracket = Component.text("[").color(NamedTextColor.GRAY);
        Component authorityText = Component.text("Authority").color(NamedTextColor.GREEN);
        Component closeBracket = Component.text("]").color(NamedTextColor.GRAY);

        Component authorityComponent = Component.empty()
                .append(openBracket)
                .append(authorityText)
                .append(closeBracket)
                .hoverEvent(HoverEvent.showText(hoverText));

        // Add to status screen
        addComponentToScreen(event, "authority_display", authorityComponent);
    }

    @Override
    public void addToTownScreen(TownStatusScreenEvent event, Town town) {
        double currentAuthority = plugin.getTownAuthorityManager().getAuthority(town);
        double dailyGain = plugin.getTownAuthorityManager().calculateDailyAuthorityGain(town);
        int residents = town.getResidents().size();

        // Create hover text component
        Component hoverText = Component.text("Town Authority Details")
                .color(NamedTextColor.DARK_GREEN)
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Current Authority: " + String.format("%.2f", currentAuthority))
                        .color(NamedTextColor.GREEN))
                .append(Component.newline())
                .append(Component.text("Daily Gain: +" + String.format("%.2f", dailyGain) + "/day")
                        .color(NamedTextColor.GREEN))
                .append(Component.newline())
                .append(Component.text("Residents: " + residents)
                        .color(NamedTextColor.GREEN));

        // Create the Authority component
        Component openBracket = Component.text("[").color(NamedTextColor.GRAY);
        Component authorityText = Component.text("Authority").color(NamedTextColor.GREEN);
        Component closeBracket = Component.text("]").color(NamedTextColor.GRAY);

        Component authorityComponent = Component.empty()
                .append(openBracket)
                .append(authorityText)
                .append(closeBracket)
                .hoverEvent(HoverEvent.showText(hoverText));

        // Add to status screen
        addComponentToScreen(event, "town_authority_display", authorityComponent);
    }

    /**
     * Update nation's authority metadata to remove default display
     */
    public void updateNationAuthorityMetadata(Nation nation) {
        try {
            // Remove Towny's default political power display
            if (nation.hasMeta(POLITICAL_POWER_KEY)) {
                nation.removeMetaData(nation.getMetadata(POLITICAL_POWER_KEY));
            }

            double currentAuthority = plugin.getAuthorityManager().getAuthority(nation);
            double dailyGain = plugin.getAuthorityManager().calculateDailyAuthorityGain(nation);
            String data = String.format("%.2f|%.2f", currentAuthority, dailyGain);

            if (nation.hasMeta(STORAGE_KEY)) {
                nation.removeMetaData(nation.getMetadata(STORAGE_KEY));
            }
            nation.addMetaData(new StringDataField(STORAGE_KEY, data));
        } catch (Exception e) {
            logger.warning("Error updating nation metadata: " + e.getMessage());
        }
    }
}