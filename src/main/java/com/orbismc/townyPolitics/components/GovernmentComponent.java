package com.orbismc.townyPolitics.components;

import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.HoverEvent;

/**
 * Component that displays Government information on status screens
 */
public class GovernmentComponent extends StatusComponent {

    public GovernmentComponent(TownyPolitics plugin) {
        super(plugin, "Government");
    }

    @Override
    public void addToNationScreen(NationStatusScreenEvent event, Nation nation) {
        // Get government type
        GovernmentType govType = plugin.getGovManager().getGovernmentType(nation);

        // Create hover text component
        Component hoverText = Component.text("Government Information")
                .color(NamedTextColor.DARK_GREEN)
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Type: " + govType.getDisplayName())
                        .color(NamedTextColor.GREEN))
                .append(Component.newline())
                .append(Component.text(govType.getDescription())
                        .color(NamedTextColor.GREEN));

        // Create the Government component
        Component openBracket = Component.text("[").color(NamedTextColor.GRAY);
        Component govText = Component.text("Government").color(NamedTextColor.GREEN);
        Component closeBracket = Component.text("]").color(NamedTextColor.GRAY);

        Component govComponent = Component.empty()
                .append(openBracket)
                .append(govText)
                .append(closeBracket)
                .hoverEvent(HoverEvent.showText(hoverText));

        // Add to status screen
        addComponentToScreen(event, "government_display", govComponent);
    }

    @Override
    public void addToTownScreen(TownStatusScreenEvent event, Town town) {
        // Get government type
        GovernmentType govType = plugin.getTownGovManager().getGovernmentType(town);

        // Create hover text component
        Component hoverText = Component.text("Government Information")
                .color(NamedTextColor.DARK_GREEN)
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Type: " + govType.getDisplayName())
                        .color(NamedTextColor.GREEN))
                .append(Component.newline())
                .append(Component.text(govType.getDescription())
                        .color(NamedTextColor.GREEN));

        // Create the Government component
        Component openBracket = Component.text("[").color(NamedTextColor.GRAY);
        Component govText = Component.text("Government").color(NamedTextColor.GREEN);
        Component closeBracket = Component.text("]").color(NamedTextColor.GRAY);

        Component govComponent = Component.empty()
                .append(openBracket)
                .append(govText)
                .append(closeBracket)
                .hoverEvent(HoverEvent.showText(hoverText));

        // Add to status screen
        addComponentToScreen(event, "government_display", govComponent);
    }
}