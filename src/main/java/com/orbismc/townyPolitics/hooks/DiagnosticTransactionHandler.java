package com.orbismc.townyPolitics.hooks;

import com.palmergames.bukkit.towny.event.economy.NationTransactionEvent;
import com.palmergames.bukkit.towny.event.economy.TownyPreTransactionEvent;
import com.palmergames.bukkit.towny.event.economy.TownyTransactionEvent;
import com.palmergames.bukkit.towny.event.time.dailytaxes.PreTownPaysNationTaxEvent;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.utils.DelegateLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class DiagnosticTransactionHandler implements Listener {
    private final TownyPolitics plugin;
    private final DelegateLogger logger;

    public DiagnosticTransactionHandler(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "Diagnostic");
    }

    @EventHandler
    public void onPreTownPaysNationTax(PreTownPaysNationTaxEvent event) {
        if (!plugin.getDebugLogger().isEnabled()) return;

        logger.info("PreTownPaysNationTaxEvent fired - " +
                event.getTown().getName() + " -> " + event.getNation().getName() +
                " amount: " + event.getTax());
    }

    @EventHandler
    public void onTownyPreTransaction(TownyPreTransactionEvent event) {
        if (!plugin.getDebugLogger().isEnabled()) return;

        logger.info("TownyPreTransactionEvent fired - " +
                "Amount: " + event.getTransaction().getAmount() +
                ", Type: " + event.getTransaction().getType());
    }

    @EventHandler
    public void onTownyTransaction(TownyTransactionEvent event) {
        if (!plugin.getDebugLogger().isEnabled()) return;

        logger.info("TownyTransactionEvent fired - " +
                "Amount: " + event.getTransaction().getAmount() +
                ", Type: " + event.getTransaction().getType());
    }

    @EventHandler
    public void onNationTransaction(NationTransactionEvent event) {
        if (!plugin.getDebugLogger().isEnabled()) return;

        logger.info("NationTransactionEvent fired - " +
                "Nation: " + event.getNation().getName() +
                ", Amount: " + event.getTransaction().getAmount() +
                ", Type: " + event.getTransaction().getType());
    }
}