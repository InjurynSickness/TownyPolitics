package com.orbismc.townyPolitics.services;

import com.orbismc.townyPolitics.government.GovernmentType;
import com.palmergames.bukkit.towny.object.Nation;
import java.util.UUID;

public interface GovernmentService {
    void setGovernmentType(UUID entityId, GovernmentType type, boolean isNation, boolean bypassCooldown);
    GovernmentType getGovernmentType(UUID entityId, boolean isNation);
    boolean isOnCooldown(UUID entityId, boolean isNation);
    long getLastChangeTime(UUID entityId, boolean isNation);
    void loadData();
}