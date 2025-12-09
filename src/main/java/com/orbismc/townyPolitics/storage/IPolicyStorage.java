package com.orbismc.townyPolitics.storage;

import com.orbismc.townyPolitics.policy.ActivePolicy;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Interface for Policy storage implementations
 */
public interface IPolicyStorage {
    /**
     * Save an active policy to storage
     * @param policy The policy to save
     */
    void saveActivePolicy(ActivePolicy policy);

    /**
     * Remove an active policy from storage
     * @param policyId The UUID of the policy to remove
     */
    void removeActivePolicy(UUID policyId);

    /**
     * Load all active policies for towns or nations
     * @param isNation Whether to load nation (true) or town (false) policies
     * @return Map of entity UUIDs to their active policies
     */
    Map<UUID, Set<ActivePolicy>> loadActivePolicies(boolean isNation);

    /**
     * Save all data
     */
    void saveAll();
}