package com.orbismc.townyPolitics.services;

import com.palmergames.bukkit.towny.object.Nation;
import java.util.UUID;

public interface PoliticalPowerService {
    double getPoliticalPower(Nation nation);
    void setPoliticalPower(Nation nation, double amount);
    void addPoliticalPower(Nation nation, double amount);
    boolean removePoliticalPower(Nation nation, double amount);
    double calculateDailyPPGain(Nation nation);
    void processNewDay();
}