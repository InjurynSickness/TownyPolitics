package com.orbismc.townyPolitics.storage;

import com.orbismc.townyPolitics.government.GovernmentType;

import java.util.Map;
import java.util.UUID;

/**
 * Interface for Government storage implementations
 */
public interface IGovernmentStorage {
    void saveGovernment(UUID uuid, GovernmentType type, boolean isNation);
    void saveChangeTime(UUID uuid, long timestamp, boolean isNation);
    GovernmentType getGovernment(UUID uuid, boolean isNation);
    long getChangeTime(UUID uuid, boolean isNation);
    Map<UUID, GovernmentType> loadAllGovernments(boolean isNation);
    Map<UUID, Long> loadAllChangeTimes(boolean isNation);
    void saveAll();
}