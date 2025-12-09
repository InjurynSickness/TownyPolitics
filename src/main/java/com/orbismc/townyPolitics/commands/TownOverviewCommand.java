package com.orbismc.townyPolitics.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Resident;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.managers.TownGovernmentManager;
import com.orbismc.townyPolitics.managers.TownAuthorityManager;
import com.orbismc.townyPolitics.managers.TownDecadenceManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TownOverviewCommand implements CommandExecutor {

    private final TownyPolitics plugin;
    private final TownGovernmentManager townGovManager;
    private final TownAuthorityManager townAuthorityManager;
    private final TownDecadenceManager townDecadenceManager;
    private final TownyAPI townyAPI;

    public TownOverviewCommand(TownyPolitics plugin, TownGovernmentManager townGovManager, 
                              TownAuthorityManager townAuthorityManager, TownDecadenceManager townDecadenceManager) {
        this.plugin = plugin;
        this.townGovManager = townGovManager;
        this.townAuthorityManager = townAuthorityManager;
        this.townDecadenceManager = townDecadenceManager;
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

        Town town;
        if (args.length > 0) {
            // Look up town by name
            town = townyAPI.getTown(args[0]);
            if (town == null) {
                player.sendMessage(Component.text("Town not found: " + args[0], NamedTextColor.RED));
                return true;
            }
        } else {
            // Use player's town
            town = resident.getTownOrNull();
            if (town == null) {
                player.sendMessage(Component.text("You are not part of a town.", NamedTextColor.RED));
                return true;
            }
        }

        showTownOverview(player, town);
        return true;
    }

    private void showTownOverview(Player player, Town town) {
        // Get government type
        GovernmentType govType = townGovManager.getGovernmentType(town);

        // Get authority
        double authority = townAuthorityManager.getAuthority(town);
        double dailyGain = townAuthorityManager.calculateDailyAuthorityGain(town);

        // Get decadence
        double decadence = townDecadenceManager.getDecadence(town);
        double decadenceGain = townDecadenceManager.calculateDailyDecadenceGain(town);

        // Display overview with custom header
        Component header = Component.text(".oOo.________.[", NamedTextColor.GOLD)
                .append(Component.text(" " + town.getName() + "'s Political Overview ", NamedTextColor.YELLOW))
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
        player.sendMessage(Component.text("Political Power: ", NamedTextColor.DARK_GREEN)
                .append(Component.text(String.format("%.2f", authority), NamedTextColor.GREEN)));
        
        player.sendMessage(Component.text("Daily Authority Gain: ", NamedTextColor.DARK_GREEN)
                .append(Component.text(String.format("+%.2f", dailyGain), NamedTextColor.GREEN)));

        // Decadence section
        NamedTextColor decadenceColor;
        if (decadence >= 75) decadenceColor = NamedTextColor.DARK_RED;
        else if (decadence >= 50) decadenceColor = NamedTextColor.RED;
        else if (decadence <= 25) decadenceColor = NamedTextColor.YELLOW;
        else decadenceColor = NamedTextColor.GREEN;

        player.sendMessage(Component.text("Corruption Level: ", NamedTextColor.DARK_RED)
                .append(Component.text(String.format("%.1f%%", decadence), decadenceColor)));
        
        player.sendMessage(Component.text("Daily Corruption Gain: ", NamedTextColor.DARK_RED)
                .append(Component.text(String.format("+%.2f%%", decadenceGain), NamedTextColor.RED)));

        // Show residents and size info
        player.sendMessage(Component.text("Residents: ", NamedTextColor.DARK_GREEN)
                .append(Component.text(String.valueOf(town.getResidents().size()), NamedTextColor.GREEN)));

        player.sendMessage(Component.text("Town Blocks: ", NamedTextColor.DARK_GREEN)
                .append(Component.text(String.valueOf(town.getTownBlocks().size()), NamedTextColor.GREEN)));
    }
}