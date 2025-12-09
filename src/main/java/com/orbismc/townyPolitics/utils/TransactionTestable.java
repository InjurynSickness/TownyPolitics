package com.orbismc.townyPolitics.utils;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;

public interface TransactionTestable {
    boolean testTransaction(Town town, Nation nation, double amount, TestCallback callback);
}