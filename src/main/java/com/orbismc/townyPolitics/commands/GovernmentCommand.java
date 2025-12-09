package com.orbismc.townyPolitics.commands;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.managers.GovernmentManager;
import com.orbismc.townyPolitics.managers.TownGovernmentManager;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GovernmentCommand implements CommandExecutor, TabCompleter {

    private final TownyPolitics plugin;
    private final GovernmentManager govManager;
    private final TownGovernmentManager townGovManager;
    private final String commandContext; // "nation" or "town"

    public GovernmentCommand(TownyPolitics plugin, String commandContext) {
        this.plugin = plugin;
        this.govManager = plugin.getGovManager();
        this.townGovManager = plugin.getTownGovManager();
        this.commandContext = commandContext; // Store the context
    }

    // Legacy constructor for backward compatibility
    public GovernmentCommand(TownyPolitics plugin) {
        this(plugin, "unknown"); // Default to unknown, will try to detect from context
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "help":
                sendHelp(player);
                break;
            case "info":
                handleInfo(player, args);
                break;
            case "set":
                handleSet(player, args);
                break;
            case "list":
                handleList(player);
                break;
            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void sendHelp(Player player) {
        String contextName = getContextName();
        player.sendMessage(Component.text(contextName + " Government Commands", NamedTextColor.BLUE, TextDecoration.BOLD));
        player.sendMessage(Component.text("/" + contextName.toLowerCase() + " government info", NamedTextColor.YELLOW)
            .append(Component.text(" - View current government info", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/" + contextName.toLowerCase() + " government set <type>", NamedTextColor.YELLOW)
            .append(Component.text(" - Change government type", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/" + contextName.toLowerCase() + " government list", NamedTextColor.YELLOW)
            .append(Component.text(" - List all government types", NamedTextColor.GRAY)));
    }

    private String getContextName() {
        if ("nation".equals(commandContext)) return "Nation";
        if ("town".equals(commandContext)) return "Town";
        return "Government"; // Fallback
    }

    private boolean isNationContext() {
        return "nation".equals(commandContext);
    }

    private boolean isTownContext() {
        return "town".equals(commandContext);
    }

    private void handleInfo(Player player, String[] args) {
        try {
            Resident resident = TownyAPI.getInstance().getResident(player);
            if (resident == null) {
                player.sendMessage(Component.text("You must be a resident to use this command.", NamedTextColor.RED));
                return;
            }

            // Show info based on context
            if (isNationContext()) {
                showNationInfo(player, resident);
            } else if (isTownContext()) {
                showTownInfo(player, resident);
            } else {
                // Legacy behavior - show both
                showTownInfo(player, resident);
                if (resident.hasTown() && resident.getTown().hasNation()) {
                    player.sendMessage(Component.empty()); // Empty line for separation
                    showNationInfo(player, resident);
                }
            }

        } catch (Exception e) {
            player.sendMessage(Component.text("Error retrieving government info: " + e.getMessage(), NamedTextColor.RED));
        }
    }

    private void showNationInfo(Player player, Resident resident) {
        try {
            if (!resident.hasTown()) {
                player.sendMessage(Component.text("You must be in a town to view nation information.", NamedTextColor.RED));
                return;
            }

            Town town = resident.getTown();
            if (!town.hasNation()) {
                player.sendMessage(Component.text("Your town is not part of a nation.", NamedTextColor.RED));
                return;
            }

            Nation nation;
            try {
                nation = town.getNation();
            } catch (Exception e) {
                player.sendMessage(Component.text("Error getting nation: " + e.getMessage(), NamedTextColor.RED));
                return;
            }
            
            GovernmentType govType = govManager.getGovernmentType(nation);
            long lastChange = govManager.getLastChangeTime(nation);
            boolean onCooldown = govManager.isOnCooldown(nation);

            player.sendMessage(Component.text("Nation Government Info - " + nation.getName(), NamedTextColor.BLUE, TextDecoration.BOLD));
            player.sendMessage(Component.text("Government Type: ", NamedTextColor.GRAY)
                .append(Component.text(govType.getDisplayName(), NamedTextColor.AQUA)));
            player.sendMessage(Component.text("Description: ", NamedTextColor.GRAY)
                .append(Component.text(govType.getDescription(), NamedTextColor.WHITE)));

            if (lastChange > 0) {
                String timeAgo = formatTimeAgo(System.currentTimeMillis() - lastChange);
                player.sendMessage(Component.text("Last Changed: ", NamedTextColor.GRAY)
                    .append(Component.text(timeAgo + " ago", NamedTextColor.AQUA)));
            }

            if (onCooldown) {
                long remaining = govManager.getCooldownTimeRemaining(nation);
                String cooldownTime = govManager.formatCooldownTime(remaining);
                player.sendMessage(Component.text("Cooldown remaining: " + cooldownTime, NamedTextColor.YELLOW));
            }

        } catch (Exception e) {
            player.sendMessage(Component.text("Error showing nation info: " + e.getMessage(), NamedTextColor.RED));
        }
    }

    private void showTownInfo(Player player, Resident resident) {
        try {
            if (!resident.hasTown()) {
                player.sendMessage(Component.text("You must be in a town to view town government information.", NamedTextColor.RED));
                return;
            }

            Town town = resident.getTown();
            GovernmentType govType = townGovManager.getGovernmentType(town);
            long lastChange = townGovManager.getLastChangeTime(town);
            boolean onCooldown = townGovManager.isOnCooldown(town);

            player.sendMessage(Component.text("Town Government Info - " + town.getName(), NamedTextColor.BLUE, TextDecoration.BOLD));
            player.sendMessage(Component.text("Government Type: ", NamedTextColor.GRAY)
                .append(Component.text(govType.getDisplayName(), NamedTextColor.AQUA)));
            player.sendMessage(Component.text("Description: ", NamedTextColor.GRAY)
                .append(Component.text(govType.getDescription(), NamedTextColor.WHITE)));

            if (lastChange > 0) {
                String timeAgo = formatTimeAgo(System.currentTimeMillis() - lastChange);
                player.sendMessage(Component.text("Last Changed: ", NamedTextColor.GRAY)
                    .append(Component.text(timeAgo + " ago", NamedTextColor.AQUA)));
            }

            if (onCooldown) {
                long remaining = townGovManager.getCooldownTimeRemaining(town);
                String cooldownTime = townGovManager.formatCooldownTime(remaining);
                player.sendMessage(Component.text("Cooldown remaining: " + cooldownTime, NamedTextColor.YELLOW));
            }

        } catch (Exception e) {
            player.sendMessage(Component.text("Error showing town info: " + e.getMessage(), NamedTextColor.RED));
        }
    }

    private void handleSet(Player player, String[] args) {
        if (args.length < 2) {
            String contextName = getContextName().toLowerCase();
            player.sendMessage(Component.text("Usage: /" + contextName + " government set <type>", NamedTextColor.RED));
            return;
        }

        try {
            Resident resident = TownyAPI.getInstance().getResident(player);
            if (resident == null) {
                player.sendMessage(Component.text("You must be a resident to use this command.", NamedTextColor.RED));
                return;
            }

            if (!resident.hasTown()) {
                player.sendMessage(Component.text("You must be in a town to change government.", NamedTextColor.RED));
                return;
            }

            Town town = resident.getTown();

            // Parse government type
            String typeName = args[1].toUpperCase();
            GovernmentType newType;
            try {
                newType = GovernmentType.valueOf(typeName);
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text("Invalid government type: " + args[1], NamedTextColor.RED));
                player.sendMessage(Component.text("Available types: ", NamedTextColor.GRAY)
                    .append(Component.text(getAvailableTypesString(), NamedTextColor.AQUA)));
                return;
            }

            // Handle based on context
            if (isNationContext()) {
                handleSetNationGovernment(player, resident, town, newType);
            } else if (isTownContext()) {
                handleSetTownGovernment(player, town, newType);
            } else {
                // Legacy behavior - try to determine from context or default to town
                player.sendMessage(Component.text("Please use /nation government set <type> for nation government or /town government set <type> for town government.", NamedTextColor.YELLOW));
                return;
            }

        } catch (Exception e) {
            player.sendMessage(Component.text("Error changing government: " + e.getMessage(), NamedTextColor.RED));
        }
    }

    private void handleSetTownGovernment(Player player, Town town, GovernmentType newType) {
        // Check if player is mayor
        try {
            Resident resident = TownyAPI.getInstance().getResident(player);
            if (!town.getMayor().equals(resident)) {
                player.sendMessage(Component.text("Only the mayor can change the town's government.", NamedTextColor.RED));
                return;
            }
        } catch (Exception e) {
            player.sendMessage(Component.text("Error checking mayor status: " + e.getMessage(), NamedTextColor.RED));
            return;
        }

        // Check if government type is valid for towns
        if (!newType.isValidForTowns()) {
            player.sendMessage(Component.text(newType.getDisplayName() + " government is not available for towns.", NamedTextColor.RED));
            return;
        }

        GovernmentType currentType = townGovManager.getGovernmentType(town);

        if (currentType == newType) {
            player.sendMessage(Component.text("Your town already has " + newType.getDisplayName() + " government.", NamedTextColor.YELLOW));
            return;
        }

        if (townGovManager.isOnCooldown(town)) {
            long remaining = townGovManager.getCooldownTimeRemaining(town);
            String cooldownTime = townGovManager.formatCooldownTime(remaining);
            player.sendMessage(Component.text("Your town cannot change government for another " + cooldownTime + ".", NamedTextColor.RED));
            return;
        }

        if (townGovManager.setGovernmentType(town, newType)) {
            player.sendMessage(Component.text("Town government changed from " + 
                currentType.getDisplayName() + " to " + newType.getDisplayName() + "!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to change town government.", NamedTextColor.RED));
        }
    }

    private void handleSetNationGovernment(Player player, Resident resident, Town town, GovernmentType newType) {
        if (!town.hasNation()) {
            player.sendMessage(Component.text("Your town is not part of a nation.", NamedTextColor.RED));
            return;
        }

        Nation nation;
        try {
            nation = town.getNation();
        } catch (Exception e) {
            player.sendMessage(Component.text("Error getting nation: " + e.getMessage(), NamedTextColor.RED));
            return;
        }

        // Check if player is nation leader
        if (!nation.getKing().equals(resident)) {
            player.sendMessage(Component.text("Only the nation leader can change the nation's government.", NamedTextColor.RED));
            return;
        }

        // Check if government type is valid for nations
        if (!newType.isValidForNations()) {
            player.sendMessage(Component.text(newType.getDisplayName() + " government is not available for nations.", NamedTextColor.RED));
            return;
        }

        GovernmentType currentType = govManager.getGovernmentType(nation);

        if (currentType == newType) {
            player.sendMessage(Component.text("Your nation already has " + newType.getDisplayName() + " government.", NamedTextColor.YELLOW));
            return;
        }

        if (govManager.isOnCooldown(nation)) {
            long remaining = govManager.getCooldownTimeRemaining(nation);
            String cooldownTime = govManager.formatCooldownTime(remaining);
            player.sendMessage(Component.text("Your nation cannot change government for another " + cooldownTime + ".", NamedTextColor.RED));
            return;
        }

        if (govManager.setGovernmentType(nation, newType)) {
            player.sendMessage(Component.text("Nation government changed from " + 
                currentType.getDisplayName() + " to " + newType.getDisplayName() + "!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to change nation government.", NamedTextColor.RED));
        }
    }

    private void handleList(Player player) {
        try {
            Resident resident = TownyAPI.getInstance().getResident(player);
            if (resident == null) {
                player.sendMessage(Component.text("You must be a resident to use this command.", NamedTextColor.RED));
                return;
            }
            
            if (isNationContext()) {
                // Show only nation government types
                player.sendMessage(Component.text("Available Nation Government Types", NamedTextColor.BLUE, TextDecoration.BOLD));
                for (GovernmentType type : GovernmentType.getNationGovernmentTypes()) {
                    player.sendMessage(Component.text(type.getDisplayName(), NamedTextColor.AQUA)
                        .append(Component.text(" - " + type.getDescription(), NamedTextColor.GRAY)));
                }
            } else if (isTownContext()) {
                // Show only town government types
                player.sendMessage(Component.text("Available Town Government Types", NamedTextColor.BLUE, TextDecoration.BOLD));
                for (GovernmentType type : GovernmentType.getTownGovernmentTypes()) {
                    player.sendMessage(Component.text(type.getDisplayName(), NamedTextColor.AQUA)
                        .append(Component.text(" - " + type.getDescription(), NamedTextColor.GRAY)));
                }
            } else {
                // Legacy behavior - show both
                boolean showTownTypes = resident.hasTown();
                boolean showNationTypes = resident.hasTown() && resident.getTown().hasNation();
                
                if (showTownTypes) {
                    player.sendMessage(Component.text("Available Town Government Types", NamedTextColor.BLUE, TextDecoration.BOLD));
                    for (GovernmentType type : GovernmentType.getTownGovernmentTypes()) {
                        player.sendMessage(Component.text(type.getDisplayName(), NamedTextColor.AQUA)
                            .append(Component.text(" - " + type.getDescription(), NamedTextColor.GRAY)));
                    }
                }
                
                if (showNationTypes) {
                    player.sendMessage(Component.text("Available Nation Government Types", NamedTextColor.BLUE, TextDecoration.BOLD));
                    for (GovernmentType type : GovernmentType.getNationGovernmentTypes()) {
                        player.sendMessage(Component.text(type.getDisplayName(), NamedTextColor.AQUA)
                            .append(Component.text(" - " + type.getDescription(), NamedTextColor.GRAY)));
                    }
                }
                
                if (!showTownTypes && !showNationTypes) {
                    player.sendMessage(Component.text("Available Government Types", NamedTextColor.BLUE, TextDecoration.BOLD));
                    for (GovernmentType type : GovernmentType.values()) {
                        player.sendMessage(Component.text(type.getDisplayName(), NamedTextColor.AQUA)
                            .append(Component.text(" - " + type.getDescription(), NamedTextColor.GRAY)));
                    }
                }
            }
        } catch (Exception e) {
            player.sendMessage(Component.text("Error listing government types: " + e.getMessage(), NamedTextColor.RED));
        }
    }

    private String getAvailableTypesString() {
        if (isNationContext()) {
            GovernmentType[] types = GovernmentType.getNationGovernmentTypes();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < types.length; i++) {
                sb.append(types[i].name().toLowerCase());
                if (i < types.length - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        } else if (isTownContext()) {
            GovernmentType[] types = GovernmentType.getTownGovernmentTypes();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < types.length; i++) {
                sb.append(types[i].name().toLowerCase());
                if (i < types.length - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        } else {
            GovernmentType[] types = GovernmentType.values();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < types.length; i++) {
                sb.append(types[i].name().toLowerCase());
                if (i < types.length - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        }
    }

    private String formatTimeAgo(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + " day" + (days != 1 ? "s" : "");
        if (hours > 0) return hours + " hour" + (hours != 1 ? "s" : "");
        if (minutes > 0) return minutes + " minute" + (minutes != 1 ? "s" : "");
        return seconds + " second" + (seconds != 1 ? "s" : "");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument: actions
            String[] actions = {"help", "info", "set", "list"};
            for (String action : actions) {
                if (action.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(action);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            // Second argument for set: government types based on context
            GovernmentType[] types;
            if (isNationContext()) {
                types = GovernmentType.getNationGovernmentTypes();
            } else if (isTownContext()) {
                types = GovernmentType.getTownGovernmentTypes();
            } else {
                types = GovernmentType.values();
            }
            
            for (GovernmentType type : types) {
                String typeName = type.name().toLowerCase();
                if (typeName.startsWith(args[1].toLowerCase())) {
                    completions.add(typeName);
                }
            }
        }

        return completions;
    }
}