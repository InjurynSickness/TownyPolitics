package com.orbismc.townyPolitics.services;

import com.orbismc.townyPolitics.policy.ActivePolicy;
import com.orbismc.townyPolitics.policy.PolicyEffects;
import com.palmergames.bukkit.towny.object.Nation;

import java.util.Set;
import java.util.UUID;

public interface PolicyService {
    boolean enactPolicy(UUID entityId, String policyId, boolean isNation);
    boolean revokePolicy(UUID entityId, UUID policyId, boolean isNation);
    Set<ActivePolicy> getActivePolicies(UUID entityId, boolean isNation);
    PolicyEffects getCombinedPolicyEffects(UUID entityId, boolean isNation);
    void processNewDay();
}