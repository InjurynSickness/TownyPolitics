package com.orbismc.townyPolitics.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.PolicyManager;
import com.orbismc.townyPolitics.policy.ActivePolicy;
import com.orbismc.townyPolitics.policy.Policy;
import com.orbismc.townyPolitics.policy.PolicyEffects;
import com.orbismc.townyPolitics.utils.PolicyEffectsDisplay;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class PolicyCommand implements CommandExecutor, TabCompleter {

    private final TownyPolitics plugin;
    private final PolicyManager policyManager;
    private final TownyAPI townyAPI;
    private final String commandSource;

    public PolicyCommand(TownyPolitics plugin, PolicyManager policyManager, String commandSource) {
        this.plugin = plugin;
        this.policyManager = policyManager;
        this.townyAPI = TownyAPI.getInstance();
        this.commandSource = commandSource; // "town" or "nation"
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
            sender.sendMessage(Component.text("You are not registered in Towny.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            showPolicyHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                return handleListCommand(player, resident, args);
            case "info":
                return handleInfoCommand(player, resident, args);
            case "enact":
                return handleEnactCommand(player, resident, args);
            case "revoke":
                return handleRevokeCommand(player, resident, args);
            default:
                player.sendMessage(Component.text("Unknown subcommand: " + subCommand, NamedTextColor.RED));
                showPolicyHelp(player);
                return true;
        }
    }

    private boolean handleListCommand(Player player, Resident resident, String[] args) {
        if (commandSource.equals("town")) {
            // Town policies
            Town town = resident.getTownOrNull();
            if (town == null) {
                player.sendMessage(Component.text("You are not part of a town.", NamedTextColor.RED));
                return true;
            }

            Set<ActivePolicy> activePolicies = policyManager.getActivePolicies(town);
            if (activePolicies.isEmpty()) {
                player.sendMessage(Component.text("Your town has no active policies.", NamedTextColor.YELLOW));
                return true;
            }

            player.sendMessage(Component.text("=== " + town.getName() + "'s Active Policies ===", NamedTextColor.GOLD));
            for (ActivePolicy activePolicy : activePolicies) {
                Policy policy = policyManager.getPolicy(activePolicy.getPolicyId());
                if (policy == null) continue;

                player.sendMessage(Component.text("• ", NamedTextColor.YELLOW)
                        .append(Component.text(policy.getName(), NamedTextColor.WHITE))
                        .append(Component.text(" (" + activePolicy.formatRemainingTime() + ")", NamedTextColor.GRAY))
                        .append(Component.text(" ID: " + activePolicy.getId(), NamedTextColor.DARK_GRAY)));
            }
        } else {
            // Nation policies
            Nation nation = resident.getNationOrNull();
            if (nation == null) {
                player.sendMessage(Component.text("You are not part of a nation.", NamedTextColor.RED));
                return true;
            }

            Set<ActivePolicy> activePolicies = policyManager.getActivePolicies(nation);
            if (activePolicies.isEmpty()) {
                player.sendMessage(Component.text("Your nation has no active policies.", NamedTextColor.YELLOW));
                return true;
            }

            player.sendMessage(Component.text("=== " + nation.getName() + "'s Active Policies ===", NamedTextColor.GOLD));
            for (ActivePolicy activePolicy : activePolicies) {
                Policy policy = policyManager.getPolicy(activePolicy.getPolicyId());
                if (policy == null) continue;

                player.sendMessage(Component.text("• ", NamedTextColor.YELLOW)
                        .append(Component.text(policy.getName(), NamedTextColor.WHITE))
                        .append(Component.text(" (" + activePolicy.formatRemainingTime() + ")", NamedTextColor.GRAY))
                        .append(Component.text(" ID: " + activePolicy.getId(), NamedTextColor.DARK_GRAY)));
            }
        }

        return true;
    }

    private boolean handleInfoCommand(Player player, Resident resident, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /" + commandSource + " policy info <policy_id>", NamedTextColor.RED));
            return true;
        }

        String policyId = args[1];
        Policy policy = policyManager.getPolicy(policyId);

        if (policy == null) {
            player.sendMessage(Component.text("Policy not found: " + policyId, NamedTextColor.RED));
            return true;
        }

        player.sendMessage(Component.text("=== Policy: " + policy.getName() + " ===", NamedTextColor.GOLD));

        // Show town-only label if applicable
        if (policy.isTownOnly()) {
            player.sendMessage(Component.text("Type: ", NamedTextColor.YELLOW)
                    .append(Component.text("[Town Only] ", NamedTextColor.AQUA))
                    .append(Component.text(policy.getType().name(), NamedTextColor.WHITE)));
        } else {
            player.sendMessage(Component.text("Type: ", NamedTextColor.YELLOW)
                    .append(Component.text(policy.getType().name(), NamedTextColor.WHITE)));
        }

        player.sendMessage(Component.text("Description: ", NamedTextColor.YELLOW)
                .append(Component.text(policy.getDescription(), NamedTextColor.WHITE)));
        
        player.sendMessage(Component.text("Cost: ", NamedTextColor.YELLOW)
                .append(Component.text(policy.getCost() + " Political Power", NamedTextColor.WHITE)));
        
        String duration = policy.getDuration() < 0 ? "Permanent" : policy.getDuration() + " days";
        player.sendMessage(Component.text("Duration: ", NamedTextColor.YELLOW)
                .append(Component.text(duration, NamedTextColor.WHITE)));

        // Show requirements
        player.sendMessage(Component.text("Requirements:", NamedTextColor.YELLOW));

        if (policy.getMinPoliticalPower() > 0) {
            player.sendMessage(Component.text("• Min Political Power: ", NamedTextColor.GRAY)
                    .append(Component.text(String.valueOf(policy.getMinPoliticalPower()), NamedTextColor.WHITE)));
        }

        if (policy.getMaxCorruption() < 100) {
            player.sendMessage(Component.text("• Max Corruption: ", NamedTextColor.GRAY)
                    .append(Component.text(policy.getMaxCorruption() + "%", NamedTextColor.WHITE)));
        }

        // Show allowed governments
        if (!policy.getAllowedGovernments().isEmpty()) {
            player.sendMessage(Component.text("Allowed Governments:", NamedTextColor.YELLOW));
            policy.getAllowedGovernments().forEach(govt ->
                    player.sendMessage(Component.text("• ", NamedTextColor.GRAY)
                            .append(Component.text(govt.getDisplayName(), NamedTextColor.WHITE)))
            );
        }

        // Show effects
        player.sendMessage(Component.text("Effects:", NamedTextColor.YELLOW));

        // Use different display methods based on context
        if (commandSource.equals("town") && policy.getEffects().hasTownEffects()) {
            // Show town-specific effects
            PolicyEffectsDisplay.displayTownEffects(player, policy.getEffects());
        } else {
            // Show standard effects
            PolicyEffectsDisplay.displayEffects(player, policy.getEffects());
        }

        return true;
    }

    private boolean handleEnactCommand(Player player, Resident resident, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /" + commandSource + " policy enact <policy_id>", NamedTextColor.RED));
            return true;
        }

        String policyId = args[1];
        Policy policy = policyManager.getPolicy(policyId);

        if (policy == null) {
            player.sendMessage(Component.text("Policy not found: " + policyId, NamedTextColor.RED));
            return true;
        }

        // Verify policy is valid for the context (town/nation)
        if (commandSource.equals("town") && !policy.isTownOnly() && !policy.getEffects().hasTownEffects() && !policy.getType().equals(Policy.PolicyType.ECONOMIC)) {
            player.sendMessage(Component.text("This policy cannot be enacted by towns.", NamedTextColor.RED));
            return true;
        } else if (commandSource.equals("nation") && policy.isTownOnly()) {
            player.sendMessage(Component.text("This policy can only be enacted by towns.", NamedTextColor.RED));
            return true;
        }

        if (commandSource.equals("town")) {
            // Town enacting policy
            Town town = resident.getTownOrNull();
            if (town == null) {
                player.sendMessage(Component.text("You are not part of a town.", NamedTextColor.RED));
                return true;
            }

            // Check if player is mayor
            if (!town.isMayor(resident)) {
                player.sendMessage(Component.text("Only the mayor can enact town policies.", NamedTextColor.RED));
                return true;
            }

            // Check cooldown
            if (policyManager.isOnCooldown(town.getUUID())) {
                long remaining = policyManager.getCooldownTimeRemaining(town.getUUID());
                String timeStr = policyManager.formatCooldownTime(town.getUUID());
                player.sendMessage(Component.text("Your town must wait " + timeStr + " before changing policies again.", NamedTextColor.RED));
                return true;
            }

            // Attempt to enact the policy
            boolean success = policyManager.enactPolicy(town, policyId);

            if (success) {
                player.sendMessage(Component.text("Successfully enacted policy: " + policy.getName(), NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("Failed to enact policy. Check requirements and try again.", NamedTextColor.RED));
            }

        } else {
            // Nation enacting policy
            Nation nation = resident.getNationOrNull();
            if (nation == null) {
                player.sendMessage(Component.text("You are not part of a nation.", NamedTextColor.RED));
                return true;
            }

            // Check if player is king
            if (!nation.isKing(resident)) {
                player.sendMessage(Component.text("Only the nation leader can enact nation policies.", NamedTextColor.RED));
                return true;
            }

            // Check cooldown
            if (policyManager.isOnCooldown(nation.getUUID())) {
                long remaining = policyManager.getCooldownTimeRemaining(nation.getUUID());
                String timeStr = policyManager.formatCooldownTime(nation.getUUID());
                player.sendMessage(Component.text("Your nation must wait " + timeStr + " before changing policies again.", NamedTextColor.RED));
                return true;
            }

            // Attempt to enact the policy
            boolean success = policyManager.enactPolicy(nation, policyId);

            if (success) {
                player.sendMessage(Component.text("Successfully enacted policy: " + policy.getName(), NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("Failed to enact policy. Check requirements and try again.", NamedTextColor.RED));
            }
        }

        return true;
    }

    private boolean handleRevokeCommand(Player player, Resident resident, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /" + commandSource + " policy revoke <policy_uuid>", NamedTextColor.RED));
            return true;
        }

        String policyUuidStr = args[1];
        UUID policyUuid;

        try {
            policyUuid = UUID.fromString(policyUuidStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Invalid policy UUID format. Use /" + commandSource + " policy list to see active policies.", NamedTextColor.RED));
            return true;
        }

        if (commandSource.equals("town")) {
            // Town revoking policy
            Town town = resident.getTownOrNull();
            if (town == null) {
                player.sendMessage(Component.text("You are not part of a town.", NamedTextColor.RED));
                return true;
            }

            // Check if player is mayor
            if (!town.isMayor(resident)) {
                player.sendMessage(Component.text("Only the mayor can revoke town policies.", NamedTextColor.RED));
                return true;
            }

            // Check cooldown
            if (policyManager.isOnCooldown(town.getUUID())) {
                long remaining = policyManager.getCooldownTimeRemaining(town.getUUID());
                String timeStr = policyManager.formatCooldownTime(town.getUUID());
                player.sendMessage(Component.text("Your town must wait " + timeStr + " before changing policies again.", NamedTextColor.RED));
                return true;
            }

            // Attempt to revoke the policy
            boolean success = policyManager.revokePolicy(town.getUUID(), policyUuid, false);

            if (success) {
                player.sendMessage(Component.text("Successfully revoked policy.", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("Failed to revoke policy. It may not exist or belong to your town.", NamedTextColor.RED));
            }

        } else {
            // Nation revoking policy
            Nation nation = resident.getNationOrNull();
            if (nation == null) {
                player.sendMessage(Component.text("You are not part of a nation.", NamedTextColor.RED));
                return true;
            }

            // Check if player is king
            if (!nation.isKing(resident)) {
                player.sendMessage(Component.text("Only the nation leader can revoke nation policies.", NamedTextColor.RED));
                return true;
            }

            // Check cooldown
            if (policyManager.isOnCooldown(nation.getUUID())) {
                long remaining = policyManager.getCooldownTimeRemaining(nation.getUUID());
                String timeStr = policyManager.formatCooldownTime(nation.getUUID());
                player.sendMessage(Component.text("Your nation must wait " + timeStr + " before changing policies again.", NamedTextColor.RED));
                return true;
            }

            // Attempt to revoke the policy
            boolean success = policyManager.revokePolicy(nation.getUUID(), policyUuid, true);

            if (success) {
                player.sendMessage(Component.text("Successfully revoked policy.", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("Failed to revoke policy. It may not exist or belong to your nation.", NamedTextColor.RED));
            }
        }

        return true;
    }

    private void showPolicyHelp(Player player) {
        player.sendMessage(Component.text("=== " + commandSource.toUpperCase() + " Policy Commands ===", NamedTextColor.GOLD));
        
        player.sendMessage(Component.text("/" + commandSource + " policy list", NamedTextColor.YELLOW)
                .append(Component.text(" - List all active policies", NamedTextColor.WHITE)));
        
        player.sendMessage(Component.text("/" + commandSource + " policy info <policy_id>", NamedTextColor.YELLOW)
                .append(Component.text(" - View detailed information about a policy", NamedTextColor.WHITE)));
        
        player.sendMessage(Component.text("/" + commandSource + " policy enact <policy_id>", NamedTextColor.YELLOW)
                .append(Component.text(" - Enact a new policy", NamedTextColor.WHITE)));
        
        player.sendMessage(Component.text("/" + commandSource + " policy revoke <policy_uuid>", NamedTextColor.YELLOW)
                .append(Component.text(" - Revoke an active policy", NamedTextColor.WHITE)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("list", "info", "enact", "revoke");
            return filterCompletions(subCommands, args[0]);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("enact")) {
                // Return appropriate policy IDs based on command source
                Collection<Policy> policies;
                if (commandSource.equals("town")) {
                    policies = policyManager.getAvailableTownPolicies();
                } else {
                    policies = policyManager.getAvailableNationPolicies();
                }

                return filterCompletions(
                        policies.stream()
                                .map(Policy::getId)
                                .collect(Collectors.toList()),
                        args[1]
                );
            } else if (args[0].equalsIgnoreCase("revoke") && sender instanceof Player) {
                // Return active policy UUIDs
                Player player = (Player) sender;
                Resident resident = townyAPI.getResident(player.getUniqueId());

                if (resident != null) {
                    Set<ActivePolicy> activePolicies;

                    if (commandSource.equals("town")) {
                        Town town = resident.getTownOrNull();
                        if (town != null) {
                            activePolicies = policyManager.getActivePolicies(town);
                        } else {
                            return completions;
                        }
                    } else {
                        Nation nation = resident.getNationOrNull();
                        if (nation != null) {
                            activePolicies = policyManager.getActivePolicies(nation);
                        } else {
                            return completions;
                        }
                    }

                    return filterCompletions(
                            activePolicies.stream()
                                    .map(policy -> policy.getId().toString())
                                    .collect(Collectors.toList()),
                            args[1]
                    );
                }
            }
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> options, String prefix) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}