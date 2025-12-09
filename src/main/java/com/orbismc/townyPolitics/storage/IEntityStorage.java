package com.orbismc.townyPolitics.storage;

import com.orbismc.townyPolitics.government.GovernmentType;
import java.util.Map;
import java.util.UUID;

/**
 * Unified storage interface for all entity data (replaces 6 separate interfaces)
 * @param <T> The entity type (Nation or Town)
 */
public interface IEntityStorage<T> {

    // Authority methods
    void saveAuthority(UUID entityUUID, double amount);
    Map<UUID, Double> loadAllAuthority();

    // Decadence methods  
    void saveDecadence(UUID entityUUID, double amount);
    Map<UUID, Double> loadAllDecadence();

    // Government methods
    void saveGovernment(UUID uuid, GovernmentType type);
    void saveChangeTime(UUID uuid, long timestamp);
    GovernmentType getGovernment(UUID uuid);
    long getChangeTime(UUID uuid);
    Map<UUID, GovernmentType> loadAllGovernments();
    Map<UUID, Long> loadAllChangeTimes();

    // Common methods
    void saveAll();
    String getEntityType(); // "NATION" or "TOWN"
}