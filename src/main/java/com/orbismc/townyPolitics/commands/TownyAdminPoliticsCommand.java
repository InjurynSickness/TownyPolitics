package com.orbismc.townyPolitics.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.command.BaseCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.AddonCommand;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.util.StringMgmt;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.managers.GovernmentManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class TownyAdminPoliticsCommand extends BaseCommand implements TabExecutor {

    private static final List<String> townyPoliticsAdminTabCompletes = Arrays.asList(
            "setgovernment", "reload");

    private final TownyPolitics plugin;
    private final GovernmentManager govManager;

    public TownyAdminPoliticsCommand(TownyPolitics plugin, GovernmentManager govManager) {
        this.plugin = plugin;
        this.govManager = govManager;

        // Register the command with Towny
        AddonCommand townyAdminPoliticsCommand = new AddonCommand(TownyCommandAddonAPI.CommandType.TOWNYADMIN, "politics", this);
        TownyCommandAddonAPI.addSubCommand(townyAdminPoliticsCommand);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        switch (args.length) {
            case 1:
                return NameUtil.filterByStart(townyPoliticsAdminTabCompletes, args[0]);
            case 2:
                if (args[0].equalsIgnoreCase("setgovernment")) {
                    return NameUtil.filterByStart(Arrays.asList("town", "nation"), args[1]);
                }
                break;
            case 3:
                if (args[0].equalsIgnoreCase("setgovernment")) {
                    if (args[1].equalsIgnoreCase("town")) {
                        return getTownyStartingWith(args[2], "t");
                    } else if (args[1].equalsIgnoreCase("nation")) {
                        return getTownyStartingWith(args[2], "n");
                    }
                }
                break;
            case 4:
                if (args[0].equalsIgnoreCase("setgovernment")) {
                    return Arrays.stream(GovernmentType.values())
                            .map(GovernmentType::name)
                            .collect(Collectors.toList());
                }
                break;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "reload" -> parseReloadCommand(sender);
                case "setgovernment", "setgov" -> parseSetGovernmentCommand(sender, StringMgmt.remFirstArg(args));
                default -> showHelp(sender);
            }
        } catch (TownyException e) {
            TownyMessaging.sendErrorMsg(sender, e.getMessage());
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        TownyMessaging.sendMessage(sender, ChatTools.formatTitle("/townyadmin politics"));
        TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/ta politics", "reload", "Reload the plugin configuration"));
        TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/ta politics", "setgovernment town [town] [type]", "Force set a town's government"));
        TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/ta politics", "setgovernment nation [nation] [type]", "Force set a nation's government"));
    }

    private void parseReloadCommand(CommandSender sender) {
        plugin.reload();
        sender.sendMessage(Component.text("TownyPolitics configuration reloaded!", NamedTextColor.GREEN));
    }

    private void parseSetGovernmentCommand(CommandSender sender, String[] args) throws TownyException {
        if (args.length < 3) {
            throw new TownyException("Not enough arguments. Use: /ta politics setgovernment [town/nation] [name] [type]");
        }

        String targetType = args[0].toLowerCase();
        String targetName = args[1];
        String govTypeName = args[2].toUpperCase();

        // Try to get the government type
        GovernmentType govType = GovernmentType.getByName(govTypeName);
        if (govType == null) {
            String availableTypes = Arrays.stream(GovernmentType.values())
                    .map(GovernmentType::name)
                    .collect(Collectors.joining(", "));
            throw new TownyException("Invalid government type: " + govTypeName + ". Available types: " + availableTypes);
        }

        // Set government based on target type
        if (targetType.equals("town")) {
            Town town = TownyUniverse.getInstance().getTown(targetName);
            if (town == null) {
                throw new TownyException("Town not found: " + targetName);
            }

            // Use town government manager for towns
            boolean success = plugin.getTownGovManager().setGovernmentType(town, govType, true);
            if (success) {
                sender.sendMessage(Component.text("Successfully set " + town.getName() + 
                        "'s government to " + govType.getDisplayName() + ".", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("Failed to set government type for " + town.getName(), NamedTextColor.RED));
            }
        } else if (targetType.equals("nation")) {
            Nation nation = TownyUniverse.getInstance().getNation(targetName);
            if (nation == null) {
                throw new TownyException("Nation not found: " + targetName);
            }

            // Force set government type and bypass cooldown
            boolean success = govManager.setGovernmentType(nation, govType, true);
            if (success) {
                sender.sendMessage(Component.text("Successfully set " + nation.getName() + 
                        "'s government to " + govType.getDisplayName() + ".", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("Failed to set government type for " + nation.getName(), NamedTextColor.RED));
            }
        } else {
            throw new TownyException("Invalid target type. Use 'town' or 'nation'.");
        }
    }
}