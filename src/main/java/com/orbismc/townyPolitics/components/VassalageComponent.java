package com.orbismc.townyPolitics.components;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.VassalageManager;
import com.orbismc.townyPolitics.vassalage.VassalageRelationship;
import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Nation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.List;

/**
 * Component that displays Vassalage information on nation status screens
 */
public class VassalageComponent extends StatusComponent {
    private final VassalageManager vassalageManager;

    public VassalageComponent(TownyPolitics plugin, VassalageManager vassalageManager) {
        super(plugin, "Vassalage");
        this.vassalageManager = vassalageManager;
    }

    @Override
    public void addToNationScreen(NationStatusScreenEvent event, Nation nation) {
        // Check if nation has vassals or is a vassal
        List<VassalageRelationship> vassals = vassalageManager.getVassals(nation);
        VassalageRelationship liegeRelationship = vassalageManager.getLiege(nation);

        if (vassals.isEmpty() && liegeRelationship == null) {
            return; // No vassalage relationships to display
        }

        // Create hover text component
        Component hoverText = Component.text("Vassalage Information")
                .color(NamedTextColor.DARK_GREEN)
                .append(Component.newline())
                .append(Component.newline());

        // Add overlord information
        if (liegeRelationship != null) {
            Nation overlord = plugin.getTownyAPI().getNation(liegeRelationship.getLiegeUUID());
            String overlordName = overlord != null ? overlord.getName() : "Unknown";
            
            hoverText = hoverText.append(Component.text("Overlord: " + overlordName)
                            .color(NamedTextColor.GOLD))
                    .append(Component.text(" (" + String.format("%.1f", liegeRelationship.getTributeRate() * 100) + "% tribute)")
                            .color(NamedTextColor.GRAY))
                    .append(Component.newline());
        }

        // Add vassals information
        if (!vassals.isEmpty()) {
            hoverText = hoverText.append(Component.text("Vassals (" + vassals.size() + "):")
                            .color(NamedTextColor.GREEN))
                    .append(Component.newline());

            for (VassalageRelationship vassal : vassals) {
                Nation vassalNation = plugin.getTownyAPI().getNation(vassal.getVassalUUID());
                String vassalName = vassalNation != null ? vassalNation.getName() : "Unknown";
                
                hoverText = hoverText.append(Component.text("• " + vassalName)
                                .color(NamedTextColor.WHITE))
                        .append(Component.text(" (" + String.format("%.1f", vassal.getTributeRate() * 100) + "%)")
                                .color(NamedTextColor.GRAY))
                        .append(Component.newline());
            }

            // Add maintenance cost
            double maintenance = vassalageManager.calculateAuthorityMaintenance(nation);
            hoverText = hoverText.append(Component.newline())
                    .append(Component.text("Authority Maintenance: " + maintenance + "/day")
                            .color(NamedTextColor.YELLOW));
        }

        // Create the component
        Component openBracket = Component.text("[").color(NamedTextColor.GRAY);
        Component vassalageText;
        
        if (liegeRelationship != null) {
            vassalageText = Component.text("Vassal").color(NamedTextColor.GOLD);
        } else {
            vassalageText = Component.text("Vassals (" + vassals.size() + ")").color(NamedTextColor.GREEN);
        }
        
        Component closeBracket = Component.text("]").color(NamedTextColor.GRAY);

        Component vassalageComponent = Component.empty()
                .append(openBracket)
                .append(vassalageText)
                .append(closeBracket)
                .hoverEvent(HoverEvent.showText(hoverText));

        // Add to status screen
        addComponentToScreen(event, "vassalage_display", vassalageComponent);
    }

    @Override
    public void addToTownScreen(com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent event, 
                               com.palmergames.bukkit.towny.object.Town town) {
        // Towns don't participate in vassalage, so no implementation needed
    }
}