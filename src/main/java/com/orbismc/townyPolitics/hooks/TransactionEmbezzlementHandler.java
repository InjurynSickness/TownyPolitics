package com.orbismc.townyPolitics.hooks;

import org.bukkit.Bukkit;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.event.economy.TownyPreTransactionEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.object.economy.transaction.Transaction;
import com.palmergames.bukkit.towny.object.economy.transaction.TransactionType;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.DecadenceManager;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionEmbezzlementHandler implements Listener {

    private final TownyPolitics plugin;
    private final DecadenceManager decadenceManager;
    private final TownyAPI townyAPI;
    private final DelegateLogger logger;

    // Transaction tracking to prevent double embezzlement
    private final Map<String, Long> recentTransactions = new ConcurrentHashMap<>();
    private static final long COOLDOWN_PERIOD = 5000L; // 5 seconds
    private final boolean sendNationMessages;

    public TransactionEmbezzlementHandler(TownyPolitics plugin) {
        this.plugin = plugin;
        this.decadenceManager = plugin.getDecadenceManager();
        this.townyAPI = TownyAPI.getInstance();
        this.logger = new DelegateLogger(plugin, "TransactionHandler");
        this.sendNationMessages = plugin.getConfig().getBoolean("corruption.notifications.embezzlement_message", true);

        // Schedule cleanup task for transaction tracking
        Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupTransactions, 6000L, 6000L); // Run every 5 minutes

        logger.info("Transaction Embezzlement Handler initialized");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreTransaction(TownyPreTransactionEvent event) {
        Transaction transaction = event.getTransaction();

        // Only target ADD transactions to nation accounts
        if (transaction.getType() != TransactionType.ADD) return;

        Account receivingAccount = transaction.getReceivingAccount();
        if (receivingAccount == null) return;

        String accountName = receivingAccount.getName();
        if (!accountName.startsWith("nation-")) return;

        // Generate a unique transaction identifier
        String transactionId = accountName + ":" + transaction.getAmount() + ":" + System.currentTimeMillis();

        // Check if this is a duplicate transaction
        if (isDuplicateTransaction(transactionId)) {
            logger.fine("Skipping duplicate transaction to " + accountName);
            return;
        }

        // Track this transaction
        trackTransaction(transactionId);

        // Get the nation from the account
        Nation nation = getNationFromAccount(receivingAccount);
        if (nation == null) {
            logger.fine("Could not find nation for account: " + accountName);
            return;
        }

        // Get corruption level
        double corruption = decadenceManager.getDecadence(nation);
        double embezzleRate = Math.min(1.0, corruption / 100.0);
        double originalAmount = transaction.getAmount();

        if (embezzleRate > 0) {
            double embezzledAmount = originalAmount * embezzleRate;
            double effectiveAmount = originalAmount - embezzledAmount;

            logger.fine(String.format("Embezzlement: %.2f (%.1f%%) of %.2f is being embezzled from %s",
                    embezzledAmount, embezzleRate * 100, originalAmount, nation.getName()));

            if (embezzleRate >= 0.99) {
                // Complete embezzlement - cancel the transaction
                event.setCancelled(true);
                logger.fine("Transaction cancelled due to 100% embezzlement");

                // Set custom message if method exists
                try {
                    if (event.getClass().getMethod("setCancelMessage", String.class) != null) {
                        event.getClass().getMethod("setCancelMessage", String.class)
                                .invoke(event, "Tax collected but completely embezzled due to corruption!");
                    }
                } catch (Exception e) {
                    logger.fine("Custom cancel message not supported");
                }
            } else if (embezzleRate > 0.01) {
                // Schedule a task to withdraw the embezzled amount after transaction
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    boolean success = nation.getAccount().withdraw(embezzledAmount,
                            "Funds embezzled due to corruption (" + corruption + "%)");

                    if (success) {
                        logger.fine("Successfully embezzled " + embezzledAmount + " from nation " + nation.getName());

                        // Send message to nation if enabled
                        if (sendNationMessages) {
                            String message = ChatColor.DARK_RED + String.format("%.2f", embezzledAmount) +
                                    ChatColor.RED + " was embezzled from the treasury due to corruption (" +
                                    String.format("%.1f", corruption) + "%)!";

                            for (org.bukkit.entity.Player player : townyAPI.getOnlinePlayers(nation)) {
                                player.sendMessage(message);
                            }
                        }
                    } else {
                        logger.warning("Failed to embezzle funds from nation " + nation.getName());
                    }
                }, 1L); // 1 tick delay
            }
        }
    }

    private boolean isDuplicateTransaction(String transactionId) {
        String[] parts = transactionId.split(":");
        if (parts.length < 2) return false;

        String matchKey = parts[0] + ":" + parts[1];
        long currentTime = System.currentTimeMillis();

        for (Map.Entry<String, Long> entry : recentTransactions.entrySet()) {
            if (entry.getKey().startsWith(matchKey) &&
                    (currentTime - entry.getValue() < COOLDOWN_PERIOD)) {
                return true;
            }
        }

        return false;
    }

    private void trackTransaction(String transactionId) {
        recentTransactions.put(transactionId, System.currentTimeMillis());
    }

    private void cleanupTransactions() {
        long currentTime = System.currentTimeMillis();
        recentTransactions.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > COOLDOWN_PERIOD);
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