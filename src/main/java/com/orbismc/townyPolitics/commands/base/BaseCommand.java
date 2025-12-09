package com.orbismc.townyPolitics.commands.base;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseCommand implements CommandExecutor, TabCompleter {
    protected final TownyPolitics plugin;
    protected final TownyAPI townyAPI;
    protected final DelegateLogger logger;

    public BaseCommand(TownyPolitics plugin, String loggerPrefix) {
        this.plugin = plugin;
        this.townyAPI = TownyAPI.getInstance();
        this.logger = new DelegateLogger(plugin, loggerPrefix);
    }

    protected boolean isPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return false;
        }
        return true;
    }

    protected Resident getResident(Player player) {
        Resident resident = townyAPI.getResident(player.getUniqueId());
        if (resident == null) {
            player.sendMessage(ChatColor.RED + "You are not registered in Towny.");
            return null;
        }
        return resident;
    }

    protected Nation getNation(Resident resident, Player player) {
        Nation nation = resident.getNationOrNull();
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "You are not part of a nation.");
            return null;
        }
        return nation;
    }

    protected Town getTown(Resident resident, Player player) {
        Town town = resident.getTownOrNull();
        if (town == null) {
            player.sendMessage(ChatColor.RED + "You are not part of a town.");
            return null;
        }
        return town;
    }

    protected boolean hasPermission(Player player, String permission) {
        if (!player.hasPermission(permission)) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return false;
        }
        return true;
    }

    protected boolean isNationLeader(Resident resident, Nation nation, Player player) {
        if (!nation.isKing(resident)) {
            player.sendMessage(ChatColor.RED + "Only the nation leader can use this command.");
            return false;
        }
        return true;
    }

    protected boolean isTownMayor(Resident resident, Town town, Player player) {
        if (!town.isMayor(resident)) {
            player.sendMessage(ChatColor.RED + "Only the mayor can use this command.");
            return false;
        }
        return true;
    }

    protected List<String> filterCompletions(List<String> options, String prefix) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}