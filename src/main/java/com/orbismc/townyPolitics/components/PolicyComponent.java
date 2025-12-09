package com.orbismc.townyPolitics.components;

import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.policy.ActivePolicy;
import com.orbismc.townyPolitics.policy.Policy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.Set;

/**
 * Component that displays Policy information on status screens
 */
public class PolicyComponent extends StatusComponent {

    public PolicyComponent(TownyPolitics plugin) {
        super(plugin, "Policy");
    }

    @Override
    public void addToNationScreen(NationStatusScreenEvent event, Nation nation) {
        Set<ActivePolicy> activePolicies = plugin.getPolicyManager().getActivePolicies(nation);

        if (activePolicies.isEmpty()) {
            return; // No active policies to display
        }

        // Create hover text component
        Component hoverText = Component.text("Active Policies")
                .color(NamedTextColor.DARK_GREEN)
                .append(Component.newline())
                .append(Component.newline());

        // Add each active policy to the hover text
        for (ActivePolicy activePolicy : activePolicies) {
            Policy policy = plugin.getPolicyManager().getPolicy(activePolicy.getPolicyId());
            if (policy == null) continue;

            hoverText = hoverText.append(Component.text("• " + policy.getName())
                            .color(NamedTextColor.GREEN))
                    .append(Component.text(" (" + activePolicy.formatRemainingTime() + ")")
                            .color(NamedTextColor.GRAY))
                    .append(Component.newline());
        }

        // Create the Policies component
        Component openBracket = Component.text("[").color(NamedTextColor.GRAY);
        Component policiesText = Component.text("Policies (" + activePolicies.size() + ")").color(NamedTextColor.BLUE);
        Component closeBracket = Component.text("]").color(NamedTextColor.GRAY);

        Component policiesComponent = Component.empty()
                .append(openBracket)
                .append(policiesText)
                .append(closeBracket)
                .hoverEvent(HoverEvent.showText(hoverText));

        // Add to status screen
        addComponentToScreen(event, "policies_display", policiesComponent);
    }

    @Override
    public void addToTownScreen(TownStatusScreenEvent event, Town town) {
        Set<ActivePolicy> activePolicies = plugin.getPolicyManager().getActivePolicies(town);

        if (activePolicies.isEmpty()) {
            return; // No active policies to display
        }

        // Create hover text component
        Component hoverText = Component.text("Active Policies")
                .color(NamedTextColor.DARK_GREEN)
                .append(Component.newline())
                .append(Component.newline());

        // Add each active policy to the hover text
        for (ActivePolicy activePolicy : activePolicies) {
            Policy policy = plugin.getPolicyManager().getPolicy(activePolicy.getPolicyId());
            if (policy == null) continue;

            hoverText = hoverText.append(Component.text("• " + policy.getName())
                            .color(NamedTextColor.GREEN))
                    .append(Component.text(" (" + activePolicy.formatRemainingTime() + ")")
                            .color(NamedTextColor.GRAY))
                    .append(Component.newline());
        }

        // Create the Policies component
        Component openBracket = Component.text("[").color(NamedTextColor.GRAY);
        Component policiesText = Component.text("Policies (" + activePolicies.size() + ")").color(NamedTextColor.BLUE);
        Component closeBracket = Component.text("]").color(NamedTextColor.GRAY);

        Component policiesComponent = Component.empty()
                .append(openBracket)
                .append(policiesText)
                .append(closeBracket)
                .hoverEvent(HoverEvent.showText(hoverText));

        // Add to status screen
        addComponentToScreen(event, "town_policies_display", policiesComponent);
    }
}