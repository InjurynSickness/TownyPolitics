package com.orbismc.townyPolitics.handlers;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.VassalageManager;
import com.orbismc.townyPolitics.utils.DelegateLogger;
import com.orbismc.townyPolitics.vassalage.VassalageRelationship;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.event.economy.TownyPreTransactionEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.object.economy.transaction.Transaction;
import com.palmergames.bukkit.towny.object.economy.transaction.TransactionType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles tribute collection from vassal nations on all incoming transactions
 * Now works like corruption - intercepts transactions before completion
 */
public class VassalageEconomicHandler implements Listener {
    private final TownyPolitics plugin;
    private final VassalageManager vassalageManager;
    private final TownyAPI townyAPI;
    private final DelegateLogger logger;

    // Track transactions to prevent double processing
    private final Map<String, Long> processedTransactions = new ConcurrentHashMap<>();
    private static final long TRANSACTION_TIMEOUT = 5000L; // 5 seconds

    public VassalageEconomicHandler(TownyPolitics plugin, VassalageManager vassalageManager) {
        this.plugin = plugin;
        this.vassalageManager = vassalageManager;
        this.townyAPI = TownyAPI.getInstance();
        this.logger = new DelegateLogger(plugin, "VassalageEconomic");
        
        // Schedule cleanup of processed transactions
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::cleanupProcessedTransactions, 
                6000L, 6000L); // Run every 5 minutes
        
        logger.info("Vassalage Economic Handler initialized - tribute collection enabled");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreTransaction(TownyPreTransactionEvent event) {
        // Check if tribute collection is enabled
        if (!plugin.getConfig().getBoolean("vassalage.tribute.enabled", true)) {
            return;
        }
        
        Transaction transaction = event.getTransaction();
        
        // Only process ADD transactions (money coming INTO accounts)
        if (transaction.getType() != TransactionType.ADD) {
            return;
        }

        Account receivingAccount = transaction.getReceivingAccount();
        if (receivingAccount == null) return;

        // Only target nation accounts
        String accountName = receivingAccount.getName();
        if (!accountName.startsWith("nation-")) return;

        // Get the nation from the account
        Nation nation = getNationFromAccount(receivingAccount);
        if (nation == null) {
            logger.fine("Could not find nation for account: " + accountName);
            return;
        }

        double amount = transaction.getAmount();
        
        // Check minimum transaction amount
        double minAmount = plugin.getConfig().getDouble("vassalage.tribute.min_transaction_amount", 0.01);
        if (amount < minAmount) {
            return;
        }

        // Check if this nation is a vassal
        VassalageRelationship relationship = vassalageManager.getLiege(nation);
        if (relationship == null) {
            return; // Not a vassal
        }

        Nation overlord = plugin.getTownyAPI().getNation(relationship.getLiegeUUID());
        if (overlord == null) {
            return; // Overlord no longer exists
        }

        // Calculate tribute
        double tributeRate = relationship.getTributeRate();
        if (tributeRate <= 0) {
            return; // No tribute to pay
        }

        double tributeAmount = amount * tributeRate;
        if (tributeAmount < minAmount) {
            return; // Tribute too small to matter
        }

        // Create a unique transaction identifier to prevent double processing
        String transactionId = nation.getUUID() + ":" + amount + ":" + System.currentTimeMillis();
        
        // Check if we already processed this transaction type recently
        long currentTime = System.currentTimeMillis();
        String transactionKey = nation.getUUID() + ":" + amount;
        
        boolean alreadyProcessed = processedTransactions.entrySet().stream()
                .anyMatch(entry -> entry.getKey().startsWith(transactionKey) && 
                         (currentTime - entry.getValue()) < TRANSACTION_TIMEOUT);
        
        if (alreadyProcessed) {
            if (plugin.getConfig().getBoolean("vassalage.debug.log_transaction_processing", false)) {
                logger.fine("Skipping duplicate tribute processing for " + nation.getName());
            }
            return;
        }

        // Mark this transaction as processed
        processedTransactions.put(transactionId, currentTime);

        // Schedule tribute collection for next tick to ensure transaction completes first
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            processTribute(nation, overlord, relationship, tributeAmount, amount);
        }, 1L);

        if (plugin.getConfig().getBoolean("vassalage.debug.log_transaction_processing", false)) {
            logger.fine("Scheduled tribute processing: " + nation.getName() + 
                       " amount: " + amount + " tribute: " + tributeAmount);
        }
    }

    private void processTribute(Nation vassal, Nation overlord, VassalageRelationship relationship, 
                               double tributeAmount, double originalAmount) {
        
        // Double-check that vassal can afford the tribute
        if (!vassal.getAccount().canPayFromHoldings(tributeAmount)) {
            if (plugin.getConfig().getBoolean("vassalage.debug.log_transaction_processing", false)) {
                logger.fine("Vassal " + vassal.getName() + " cannot afford tribute of " + tributeAmount + 
                           " after transaction processed");
            }
            return;
        }

        // Transfer tribute from vassal to overlord
        boolean success = vassal.getAccount().withdraw(tributeAmount, 
                "Tribute payment to " + overlord.getName() + " (" + 
                String.format("%.1f", relationship.getTributeRate() * 100) + "%)");
        
        if (success) {
            // Deposit to overlord
            overlord.getAccount().deposit(tributeAmount, 
                    "Tribute from vassal " + vassal.getName());
            
            // Log tribute payment if enabled
            if (plugin.getConfig().getBoolean("vassalage.debug.log_tribute_payments", true)) {
                logger.info("Tribute paid: " + vassal.getName() + " -> " + overlord.getName() + 
                           " amount: " + String.format("%.2f", tributeAmount) + 
                           " (rate: " + String.format("%.1f", relationship.getTributeRate() * 100) + "%" +
                           ", from transaction: " + String.format("%.2f", originalAmount) + ")");
            }

            // Update last tribute time
            relationship.setLastTributeTime(System.currentTimeMillis());
            vassalageManager.saveRelationship(relationship);

            // Notify vassal nation's online players if enabled
            if (plugin.getConfig().getBoolean("vassalage.tribute.notify_vassal", true)) {
                for (org.bukkit.entity.Player player : plugin.getTownyAPI().getOnlinePlayers(vassal)) {
                    player.sendMessage(org.bukkit.ChatColor.YELLOW + 
                            String.format("%.2f", tributeAmount) + " was paid as tribute to " + 
                            overlord.getName() + " (" + 
                            String.format("%.1f", relationship.getTributeRate() * 100) + "%)");
                }
            }

            // Notify overlord nation's online players if enabled
            if (plugin.getConfig().getBoolean("vassalage.tribute.notify_overlord", true)) {
                for (org.bukkit.entity.Player player : plugin.getTownyAPI().getOnlinePlayers(overlord)) {
                    player.sendMessage(org.bukkit.ChatColor.GREEN + 
                            "Received " + String.format("%.2f", tributeAmount) + " tribute from " + 
                            vassal.getName());
                }
            }
        } else {
            logger.warning("Failed to withdraw tribute from " + vassal.getName() + 
                          " amount: " + tributeAmount);
        }
    }

    /**
     * Clean up old processed transaction records to prevent memory leaks
     */
    private void cleanupProcessedTransactions() {
        long cutoff = System.currentTimeMillis() - TRANSACTION_TIMEOUT;
        processedTransactions.entrySet().removeIf(entry -> entry.getValue() < cutoff);
        
        if (logger.isEnabled()) {
            logger.fine("Cleaned up processed transactions, remaining: " + processedTransactions.size());
        }
    }

    private Nation getNationFromAccount(Account account) {
        if (account == null) return null;

        String name = account.getName();
        if (name == null) return null;

        // Try using UUID method first
        UUID uuid = TownyEconomyHandler.getTownyObjectUUID(name);
        if (uuid != null) {
            Nation nation = townyAPI.getNation(uuid);
            if (nation != null) return nation;
        }

        // Fallback to name-based lookup
        if (name.toLowerCase().startsWith("nation-")) {
            String nationName = name.substring(7); // Remove "nation-" prefix
            return townyAPI.getNation(nationName);
        }

        return null;
    }
}