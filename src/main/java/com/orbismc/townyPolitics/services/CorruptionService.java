package com.orbismc.townyPolitics.services;

import com.palmergames.bukkit.towny.object.Nation;
import java.util.UUID;

public interface CorruptionService {
    double getCorruption(Nation nation);
    void setCorruption(Nation nation, double amount);
    void addCorruption(Nation nation, double amount);
    void reduceCorruption(Nation nation, double amount);
    double calculateDailyCorruptionGain(Nation nation);
    int getCorruptionThresholdLevel(Nation nation);
    boolean isCorruptionCritical(Nation nation);
    double getTaxationModifier(Nation nation);
    double getPoliticalPowerModifier(Nation nation);
    double getResourceModifier(Nation nation);
    double getSpendingModifier(Nation nation);
    void processNewDay();
}