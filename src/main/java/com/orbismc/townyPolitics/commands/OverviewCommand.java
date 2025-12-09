package com.orbismc.townyPolitics.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.managers.GovernmentManager;
import com.orbismc.townyPolitics.managers.AuthorityManager;
import com.orbismc.townyPolitics.managers.DecadenceManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OverviewCommand implements CommandExecutor {

    private final TownyPolitics plugin;
    private final GovernmentManager govManager;
    private final AuthorityManager authorityManager;
    private final DecadenceManager decadenceManager;
    private final TownyAPI townyAPI;

    public OverviewCommand(TownyPolitics plugin, GovernmentManager govManager, AuthorityManager authorityManager, DecadenceManager decadenceManager) {
        this.plugin = plugin;
        this.govManager = govManager;
        this.authorityManager = authorityManager;
        this.decadenceManager = decadenceManager;
        this.townyAPI = TownyAPI.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        // Check if player is registered in Towny
        Resident resident = townyAPI.getResident(player.getUniqueId());
        if (resident == null) {
            player.sendMessage(Component.text("You are not registered in Towny.", NamedTextColor.RED));
            return true;
        }

        Nation nation;
        if (args.length > 0) {
            // Look up nation by name
            nation = townyAPI.getNation(args[0]);
            if (nation == null) {
                player.sendMessage(Component.text("Nation not found: " + args[0], NamedTextColor.RED));
                return true;
            }
        } else {
            // Use player's nation
            nation = resident.getNationOrNull();
            if (nation == null) {
                player.sendMessage(Component.text("You are not part of a nation.", NamedTextColor.RED));
                return true;
            }
        }

        showNationOverview(player, nation);
        return true;
    }

    private void showNationOverview(Player player, Nation nation) {
        // Get government type
        GovernmentType govType = govManager.getGovernmentType(nation);

        // Get authority
        double authority = authorityManager.getAuthority(nation);
        double dailyGain = authorityManager.calculateDailyAuthorityGain(nation);

        // Get decadence
        double decadence = decadenceManager.getDecadence(nation);
        double decadenceGain = decadenceManager.calculateDailyDecadenceGain(nation);
        boolean isCritical = decadenceManager.isDecadenceCritical(nation);

        // Display overview with custom header
        Component header = Component.text(".oOo.________.[", NamedTextColor.GOLD)
                .append(Component.text(" " + nation.getName() + "'s Political Overview ", NamedTextColor.YELLOW))
                .append(Component.text("].________.oOo.", NamedTextColor.GOLD));
        player.sendMessage(header);

        // Government section
        player.sendMessage(Component.text("Government: ", NamedTextColor.DARK_GREEN)
                .append(Component.text(govType.getDisplayName(), NamedTextColor.GREEN)));

        // Format the description to show effects title in dark green and effects in green
        String[] descLines = govType.getDescription().split("\n");
        for (String line : descLines) {
            if (line.contains("Effects:")) {
                player.sendMessage(Component.text(line, NamedTextColor.DARK_GREEN));
            } else {
                player.sendMessage(Component.text("  " + line, NamedTextColor.GREEN));
            }
        }

        // Authority section
        player.sendMessage(Component.text("Authority: ", NamedTextColor.DARK_GREEN)
                .append(Component.text(String.format("%.2f", authority), NamedTextColor.GREEN)));
        
        player.sendMessage(Component.text("Daily Authority Gain: ", NamedTextColor.DARK_GREEN)
                .append(Component.text(String.format("+%.2f", dailyGain), NamedTextColor.GREEN)));

        // Decadence section
        NamedTextColor decadenceColor;
        if (decadence >= 75) decadenceColor = NamedTextColor.DARK_RED;
        else if (decadence >= 50) decadenceColor = NamedTextColor.RED;
        else if (decadence <= 25) decadenceColor = NamedTextColor.YELLOW;
        else decadenceColor = NamedTextColor.GREEN;

        player.sendMessage(Component.text("Decadence Level: ", NamedTextColor.DARK_RED)
                .append(Component.text(String.format("%.1f%%", decadence), decadenceColor)));
        
        player.sendMessage(Component.text("Daily Decadence Gain: ", NamedTextColor.DARK_RED)
                .append(Component.text(String.format("+%.2f%%", decadenceGain), NamedTextColor.RED)));
    }

    private Component formatModifier(double modifier) {
        String formattedValue = String.format("%+.1f%%", (modifier - 1.0) * 100);

        if (modifier > 1.0) {
            return Component.text(formattedValue, NamedTextColor.RED);
        } else if (modifier < 1.0) {
            return Component.text(formattedValue, NamedTextColor.RED);
        } else {
            return Component.text("0%", NamedTextColor.GREEN);
        }
    }
}