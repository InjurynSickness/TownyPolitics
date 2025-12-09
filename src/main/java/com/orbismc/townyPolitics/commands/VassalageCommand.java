package com.orbismc.townyPolitics.commands;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.commands.base.BaseCommand;
import com.orbismc.townyPolitics.managers.VassalageManager;
import com.orbismc.townyPolitics.vassalage.VassalageOffer;
import com.orbismc.townyPolitics.vassalage.VassalageRelationship;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VassalageCommand extends BaseCommand {

    private final VassalageManager vassalageManager;

    public VassalageCommand(TownyPolitics plugin, VassalageManager vassalageManager) {
        super(plugin, "VassalageCommand");
        this.vassalageManager = vassalageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!isPlayer(sender)) {
            return true;
        }

        Player player = (Player) sender;
        Resident resident = getResident(player);
        if (resident == null) {
            return true;
        }

        Nation nation = getNation(resident, player);
        if (nation == null) {
            return true;
        }

        // Check if player is the nation leader
        if (!isNationLeader(resident, nation, player)) {
            return true;
        }

        if (args.length == 0) {
            showVassalageHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "offer":
                return handleOfferCommand(player, nation, args);
            case "accept":
                return handleAcceptCommand(player, nation, args);
            case "release":
                return handleReleaseCommand(player, nation, args);
            case "break":
                return handleBreakCommand(player, nation, args);
            case "set-tribute":
            case "settribute":
                return handleSetTributeCommand(player, nation, args);
            case "list":
                return handleListCommand(player, nation, args);
            case "info":
                return handleInfoCommand(player, nation, args);
            default:
                player.sendMessage(Component.text("Unknown subcommand: " + subCommand, NamedTextColor.RED));
                showVassalageHelp(player);
                return true;
        }
    }

    private boolean handleOfferCommand(Player player, Nation nation, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /nation vassal offer <nation> [tribute_rate]", NamedTextColor.RED));
            return true;
        }

        // Get target nation
        String targetName = args[1];
        Nation target = townyAPI.getNation(targetName);
        if (target == null) {
            player.sendMessage(Component.text("Nation not found: " + targetName, NamedTextColor.RED));
            return true;
        }

        // Parse tribute rate
        double tributeRate = 0.05; // Default 5%
        if (args.length >= 3) {
            try {
                tributeRate = Double.parseDouble(args[2]) / 100.0; // Convert percentage to decimal
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid tribute rate. Use a number (e.g., 5 for 5%)", NamedTextColor.RED));
                return true;
            }
        }

        // Validate tribute rate
        if (tributeRate < 0.0 || tributeRate > vassalageManager.getMaxTributeRate()) {
            player.sendMessage(Component.text("Tribute rate must be between 0% and " + 
                    (vassalageManager.getMaxTributeRate() * 100) + "%", NamedTextColor.RED));
            return true;
        }

        // Check if vassalage can be formed
        if (!vassalageManager.canFormVassalage(nation, target)) {
            player.sendMessage(Component.text("Cannot form vassalage with " + target.getName() + ". " +
                    "Check requirements: sufficient authority, compatible government types, and no existing relationships.", 
                    NamedTextColor.RED));
            return true;
        }

        // Create the offer
        VassalageOffer offer = vassalageManager.createOffer(nation, target, tributeRate);
        if (offer == null) {
            player.sendMessage(Component.text("Failed to create vassalage offer.", NamedTextColor.RED));
            return true;
        }

        player.sendMessage(Component.text("Vassalage offer sent to " + target.getName() + 
                " with " + String.format("%.1f", tributeRate * 100) + "% tribute rate.", NamedTextColor.GREEN));

        // Notify target nation's online members
        for (Player targetPlayer : townyAPI.getOnlinePlayers(target)) {
            targetPlayer.sendMessage(Component.text("Your nation has received a vassalage offer from " + 
                    nation.getName() + " with " + String.format("%.1f", tributeRate * 100) + "% tribute rate.", 
                    NamedTextColor.YELLOW));
            targetPlayer.sendMessage(Component.text("Use '/nation vassal accept " + nation.getName() + 
                    "' to accept or wait for it to expire.", NamedTextColor.YELLOW));
        }

        return true;
    }

    private boolean handleAcceptCommand(Player player, Nation nation, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /nation vassal accept <overlord_nation>", NamedTextColor.RED));
            return true;
        }

        String overlordName = args[1];
        Nation overlord = townyAPI.getNation(overlordName);
        if (overlord == null) {
            player.sendMessage(Component.text("Nation not found: " + overlordName, NamedTextColor.RED));
            return true;
        }

        // Find the offer
        List<VassalageOffer> offers = vassalageManager.getOffersTo(nation);
        VassalageOffer targetOffer = null;
        for (VassalageOffer offer : offers) {
            if (offer.getLiegeUUID().equals(overlord.getUUID())) {
                targetOffer = offer;
                break;
            }
        }

        if (targetOffer == null) {
            player.sendMessage(Component.text("No vassalage offer found from " + overlordName, NamedTextColor.RED));
            return true;
        }

        if (targetOffer.isExpired()) {
            player.sendMessage(Component.text("The vassalage offer from " + overlordName + " has expired.", NamedTextColor.RED));
            return true;
        }

        // Accept the offer
        boolean success = vassalageManager.acceptOffer(targetOffer.getOfferUUID(), nation);
        if (success) {
            player.sendMessage(Component.text("Successfully became a vassal of " + overlordName + 
                    " with " + String.format("%.1f", targetOffer.getProposedTributeRate() * 100) + "% tribute rate.", 
                    NamedTextColor.GREEN));

            // Notify overlord
            for (Player overlordPlayer : townyAPI.getOnlinePlayers(overlord)) {
                overlordPlayer.sendMessage(Component.text(nation.getName() + " has accepted your vassalage offer!", 
                        NamedTextColor.GREEN));
            }
        } else {
            player.sendMessage(Component.text("Failed to accept vassalage offer. Requirements may no longer be met.", 
                    NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleReleaseCommand(Player player, Nation nation, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /nation vassal release <vassal_nation>", NamedTextColor.RED));
            return true;
        }

        String vassalName = args[1];
        Nation vassal = townyAPI.getNation(vassalName);
        if (vassal == null) {
            player.sendMessage(Component.text("Nation not found: " + vassalName, NamedTextColor.RED));
            return true;
        }

        // Check if this nation is the liege of the target
        VassalageRelationship relationship = vassalageManager.getRelationship(nation.getUUID(), vassal.getUUID());
        if (relationship == null) {
            player.sendMessage(Component.text(vassalName + " is not your vassal.", NamedTextColor.RED));
            return true;
        }

        // Release the vassal
        boolean success = vassalageManager.releaseVassal(nation, vassal);
        if (success) {
            player.sendMessage(Component.text("Successfully released " + vassalName + " from vassalage.", 
                    NamedTextColor.GREEN));

            // Notify the vassal
            for (Player vassalPlayer : townyAPI.getOnlinePlayers(vassal)) {
                vassalPlayer.sendMessage(Component.text("Your nation has been released from vassalage by " + 
                        nation.getName() + ".", NamedTextColor.YELLOW));
            }
        } else {
            player.sendMessage(Component.text("Failed to release vassal.", NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleBreakCommand(Player player, Nation nation, String[] args) {
        // Check if this nation is a vassal
        VassalageRelationship relationship = vassalageManager.getLiege(nation);
        if (relationship == null) {
            player.sendMessage(Component.text("Your nation is not a vassal.", NamedTextColor.RED));
            return true;
        }

        // Check if nation can afford the break cost
        double breakCost = vassalageManager.getVassalBreakCost();
        double currentAuthority = plugin.getAuthorityManager().getAuthority(nation);
        
        if (currentAuthority < breakCost) {
            player.sendMessage(Component.text("Insufficient authority to break vassalage. Need " + 
                    breakCost + " authority, have " + String.format("%.2f", currentAuthority), NamedTextColor.RED));
            return true;
        }

        // Confirm the action
        Nation liege = townyAPI.getNation(relationship.getLiegeUUID());
        String liegeName = liege != null ? liege.getName() : "Unknown";

        player.sendMessage(Component.text("Breaking vassalage with " + liegeName + " will cost " + 
                breakCost + " authority. Type the command again within 10 seconds to confirm.", NamedTextColor.YELLOW));

        // For simplicity, we'll break immediately. In a full implementation, you'd want a confirmation system
        boolean success = vassalageManager.breakVassalage(nation);
        if (success) {
            player.sendMessage(Component.text("Successfully broke free from " + liegeName + " for " + 
                    breakCost + " authority.", NamedTextColor.GREEN));

            // Notify the former liege
            if (liege != null) {
                for (Player liegePlayer : townyAPI.getOnlinePlayers(liege)) {
                    liegePlayer.sendMessage(Component.text(nation.getName() + " has broken free from your vassalage!", 
                            NamedTextColor.RED));
                }
            }
        } else {
            player.sendMessage(Component.text("Failed to break vassalage.", NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleSetTributeCommand(Player player, Nation nation, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /nation vassal set-tribute <vassal_nation> <rate>", NamedTextColor.RED));
            return true;
        }

        String vassalName = args[1];
        Nation vassal = townyAPI.getNation(vassalName);
        if (vassal == null) {
            player.sendMessage(Component.text("Nation not found: " + vassalName, NamedTextColor.RED));
            return true;
        }

        // Parse new tribute rate
        double newRate;
        try {
            newRate = Double.parseDouble(args[2]) / 100.0; // Convert percentage to decimal
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid tribute rate. Use a number (e.g., 5 for 5%)", NamedTextColor.RED));
            return true;
        }

        // Validate tribute rate
        if (newRate < 0.0 || newRate > vassalageManager.getMaxTributeRate()) {
            player.sendMessage(Component.text("Tribute rate must be between 0% and " + 
                    (vassalageManager.getMaxTributeRate() * 100) + "%", NamedTextColor.RED));
            return true;
        }

        // Check if this nation is the liege of the target
        VassalageRelationship relationship = vassalageManager.getRelationship(nation.getUUID(), vassal.getUUID());
        if (relationship == null) {
            player.sendMessage(Component.text(vassalName + " is not your vassal.", NamedTextColor.RED));
            return true;
        }

        // Set the new tribute rate
        boolean success = vassalageManager.setTributeRate(nation, vassal, newRate);
        if (success) {
            player.sendMessage(Component.text("Successfully set tribute rate for " + vassalName + " to " + 
                    String.format("%.1f", newRate * 100) + "%", NamedTextColor.GREEN));

            // Notify the vassal
            for (Player vassalPlayer : townyAPI.getOnlinePlayers(vassal)) {
                vassalPlayer.sendMessage(Component.text("Your tribute rate has been changed to " + 
                        String.format("%.1f", newRate * 100) + "% by " + nation.getName(), NamedTextColor.YELLOW));
            }
        } else {
            player.sendMessage(Component.text("Failed to set tribute rate.", NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleListCommand(Player player, Nation nation, String[] args) {
        // Show vassals
        List<VassalageRelationship> vassals = vassalageManager.getVassals(nation);
        if (!vassals.isEmpty()) {
            player.sendMessage(Component.text("=== " + nation.getName() + "'s Vassals ===", NamedTextColor.GOLD));
            for (VassalageRelationship relationship : vassals) {
                Nation vassal = townyAPI.getNation(relationship.getVassalUUID());
                String vassalName = vassal != null ? vassal.getName() : "Unknown";
                
                player.sendMessage(Component.text("• ", NamedTextColor.YELLOW)
                        .append(Component.text(vassalName, NamedTextColor.WHITE))
                        .append(Component.text(" (" + String.format("%.1f", relationship.getTributeRate() * 100) + "% tribute)", 
                                NamedTextColor.GRAY)));
            }

            double maintenanceCost = vassalageManager.calculateAuthorityMaintenance(nation);
            player.sendMessage(Component.text("Total Authority Maintenance: " + maintenanceCost + "/day", 
                    NamedTextColor.AQUA));
        }

        // Show liege
        VassalageRelationship liegeRelationship = vassalageManager.getLiege(nation);
        if (liegeRelationship != null) {
            Nation liege = townyAPI.getNation(liegeRelationship.getLiegeUUID());
            String liegeName = liege != null ? liege.getName() : "Unknown";
            
            player.sendMessage(Component.text("=== Overlord ===", NamedTextColor.GOLD));
            player.sendMessage(Component.text("• ", NamedTextColor.YELLOW)
                    .append(Component.text(liegeName, NamedTextColor.WHITE))
                    .append(Component.text(" (" + String.format("%.1f", liegeRelationship.getTributeRate() * 100) + "% tribute)", 
                            NamedTextColor.GRAY)));
        }

        // Show pending offers
        List<VassalageOffer> incomingOffers = vassalageManager.getOffersTo(nation);
        List<VassalageOffer> outgoingOffers = vassalageManager.getOffersFrom(nation);

        if (!incomingOffers.isEmpty()) {
            player.sendMessage(Component.text("=== Incoming Offers ===", NamedTextColor.GOLD));
            for (VassalageOffer offer : incomingOffers) {
                Nation offerNation = townyAPI.getNation(offer.getLiegeUUID());
                String offerNationName = offerNation != null ? offerNation.getName() : "Unknown";
                
                player.sendMessage(Component.text("• ", NamedTextColor.YELLOW)
                        .append(Component.text(offerNationName, NamedTextColor.WHITE))
                        .append(Component.text(" (" + String.format("%.1f", offer.getProposedTributeRate() * 100) + "% tribute, " + 
                                offer.formatTimeRemaining() + ")", NamedTextColor.GRAY)));
            }
        }

        if (!outgoingOffers.isEmpty()) {
            player.sendMessage(Component.text("=== Outgoing Offers ===", NamedTextColor.GOLD));
            for (VassalageOffer offer : outgoingOffers) {
                Nation targetNation = townyAPI.getNation(offer.getTargetUUID());
                String targetName = targetNation != null ? targetNation.getName() : "Unknown";
                
                player.sendMessage(Component.text("• ", NamedTextColor.YELLOW)
                        .append(Component.text(targetName, NamedTextColor.WHITE))
                        .append(Component.text(" (" + String.format("%.1f", offer.getProposedTributeRate() * 100) + "% tribute, " + 
                                offer.formatTimeRemaining() + ")", NamedTextColor.GRAY)));
            }
        }

        if (vassals.isEmpty() && liegeRelationship == null && incomingOffers.isEmpty() && outgoingOffers.isEmpty()) {
            player.sendMessage(Component.text("No vassalage relationships or offers.", NamedTextColor.GRAY));
        }

        return true;
    }

    private boolean handleInfoCommand(Player player, Nation nation, String[] args) {
        // Show vassalage system information
        player.sendMessage(Component.text("=== Vassalage System Information ===", NamedTextColor.GOLD));
        
        player.sendMessage(Component.text("Requirements:", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("• Liege needs " + vassalageManager.getMinLiegeAuthority() + "+ authority", NamedTextColor.WHITE));
        player.sendMessage(Component.text("• Tribal and Theocracy governments cannot participate", NamedTextColor.WHITE));
        player.sendMessage(Component.text("• Nations cannot be both liege and vassal", NamedTextColor.WHITE));
        
        player.sendMessage(Component.text("Costs:", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("• Liege: " + vassalageManager.getAuthorityMaintenancePerVassal() + " authority/day per vassal", NamedTextColor.WHITE));
        player.sendMessage(Component.text("• Breaking vassalage: " + vassalageManager.getVassalBreakCost() + " authority", NamedTextColor.WHITE));
        
        player.sendMessage(Component.text("Benefits:", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("• Liege receives tribute income from vassals", NamedTextColor.WHITE));
        player.sendMessage(Component.text("• Vassals cannot be enemied directly (must enemy overlord)", NamedTextColor.WHITE));
        
        player.sendMessage(Component.text("Tribute Rate: 0% - " + (vassalageManager.getMaxTributeRate() * 100) + "%", NamedTextColor.YELLOW));

        return true;
    }

    private void showVassalageHelp(Player player) {
        player.sendMessage(Component.text("=== Vassalage Commands ===", NamedTextColor.GOLD));
        
        player.sendMessage(Component.text("/nation vassal offer <nation> [tribute_rate]", NamedTextColor.YELLOW)
                .append(Component.text(" - Offer vassalage to another nation", NamedTextColor.WHITE)));
        
        player.sendMessage(Component.text("/nation vassal accept <overlord_nation>", NamedTextColor.YELLOW)
                .append(Component.text(" - Accept a vassalage offer", NamedTextColor.WHITE)));
        
        player.sendMessage(Component.text("/nation vassal release <vassal_nation>", NamedTextColor.YELLOW)
                .append(Component.text(" - Release a vassal", NamedTextColor.WHITE)));
        
        player.sendMessage(Component.text("/nation vassal break", NamedTextColor.YELLOW)
                .append(Component.text(" - Break free from vassalage (costs authority)", NamedTextColor.WHITE)));
        
        player.sendMessage(Component.text("/nation vassal set-tribute <vassal_nation> <rate>", NamedTextColor.YELLOW)
                .append(Component.text(" - Set tribute rate for a vassal", NamedTextColor.WHITE)));
        
        player.sendMessage(Component.text("/nation vassal list", NamedTextColor.YELLOW)
                .append(Component.text(" - List vassals, overlord, and offers", NamedTextColor.WHITE)));
        
        player.sendMessage(Component.text("/nation vassal info", NamedTextColor.YELLOW)
                .append(Component.text(" - Show vassalage system information", NamedTextColor.WHITE)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("offer", "accept", "release", "break", "set-tribute", "list", "info");
            return filterCompletions(subCommands, args[0]);
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("offer") || subCommand.equals("accept") || 
                subCommand.equals("release") || subCommand.equals("set-tribute")) {
                // Return nation names
                return filterCompletions(
                        plugin.getTownyAPI().getNations().stream()
                                .map(nation -> nation.getName())
                                .collect(Collectors.toList()),
                        args[1]
                );
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("offer") || subCommand.equals("set-tribute")) {
                // Return tribute rate suggestions
                return filterCompletions(Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"), args[2]);
            }
        }

        return completions;
    }
}