package com.orbismc.townyPolitics.api;

import com.orbismc.townyPolitics.TownyPolitics;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.policy.ActivePolicy;
import com.orbismc.townyPolitics.policy.Policy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.Set;

public class TownyPoliticsAPI {

    private static TownyPoliticsAPI instance;
    private final TownyPolitics plugin;

    /**
     * Private constructor to enforce singleton pattern.
     * @param plugin The instance of the main TownyPolitics plugin.
     */
    private TownyPoliticsAPI(TownyPolitics plugin) {
        this.plugin = plugin;
    }

    /**
     * Gets the singleton instance of the TownyPoliticsAPI.
     *
     * It's recommended to check if the returned instance is null,
     * especially during server startup or if TownyPolitics might be disabled.
     *
     * @return The TownyPoliticsAPI instance, or null if TownyPolitics is not enabled.
     */
    public static synchronized TownyPoliticsAPI getInstance() {
        if (instance == null) {
            try {
                TownyPolitics mainPlugin = JavaPlugin.getPlugin(TownyPolitics.class);
                if (mainPlugin != null && mainPlugin.isEnabled()) {
                    instance = new TownyPoliticsAPI(mainPlugin);
                } else {
                    // Log a warning only if getInstance is called after server load potentially
                    if (Bukkit.getServer().getPluginManager().isPluginEnabled("TownyPolitics")) {
                        Bukkit.getLogger().warning("[YourOtherPlugin] TownyPoliticsAPI called but TownyPolitics might not be fully enabled yet!");
                    }
                    return null; // Return null if plugin isn't ready
                }
            } catch (IllegalStateException e) {
                // This can happen if called during server shutdown or before plugins are loaded
                Bukkit.getLogger().warning("[YourOtherPlugin] TownyPoliticsAPI.getInstance() called at an invalid time (server stopping or plugin disabled?).");
                return null;
            }
        }
        return instance;
    }

    // --- Nation Methods ---

    /**
     * Gets the current authority of a nation.
     *
     * @param nation The nation to check. Cannot be null.
     * @return The nation's authority, or 0.0 if the Authority manager isn't available.
     * @throws NullPointerException if nation is null.
     */
    public double getNationAuthority(Nation nation) {
        if (nation == null) throw new NullPointerException("Nation cannot be null");
        if (plugin.getAuthorityManager() == null) {
            plugin.getLogger().warning("TownyPoliticsAPI: AuthorityManager is not available!");
            return 0.0;
        }
        return plugin.getAuthorityManager().getAuthority(nation);
    }

    /**
     * Gets the current government type of a nation.
     *
     * @param nation The nation to check. Cannot be null.
     * @return The nation's GovernmentType. Returns {@link GovernmentType#TRIBAL} if the government manager isn't available.
     * @throws NullPointerException if nation is null.
     */
    public GovernmentType getNationGovernmentType(Nation nation) {
        if (nation == null) throw new NullPointerException("Nation cannot be null");
        if (plugin.getGovManager() == null) {
            plugin.getLogger().warning("TownyPoliticsAPI: GovernmentManager is not available!");
            return GovernmentType.TRIBAL;
        }
        return plugin.getGovManager().getGovernmentType(nation);
    }

    /**
     * Gets the current decadence level of a nation.
     *
     * @param nation The nation to check. Cannot be null.
     * @return The nation's decadence level (0.0 - 100.0), or 0.0 if the decadence manager isn't available.
     * @throws NullPointerException if nation is null.
     */
    public double getNationDecadence(Nation nation) {
        if (nation == null) throw new NullPointerException("Nation cannot be null");
        if (plugin.getDecadenceManager() == null) {
            plugin.getLogger().warning("TownyPoliticsAPI: DecadenceManager is not available!");
            return 0.0;
        }
        return plugin.getDecadenceManager().getDecadence(nation);
    }

    /**
     * Gets the set of active policies for a nation.
     *
     * @param nation The nation to check. Cannot be null.
     * @return An unmodifiable set of {@link ActivePolicy} objects, or an empty set if none are active or the policy manager isn't available.
     * @throws NullPointerException if nation is null.
     */
    public Set<ActivePolicy> getActiveNationPolicies(Nation nation) {
        if (nation == null) throw new NullPointerException("Nation cannot be null");
        if (plugin.getPolicyManager() == null) {
            plugin.getLogger().warning("TownyPoliticsAPI: PolicyManager is not available!");
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(plugin.getPolicyManager().getActivePolicies(nation));
    }

    // --- Town Methods ---

    /**
     * Gets the current authority of a town.
     * Note: This depends on the town-level authority system being enabled in TownyPolitics config.
     *
     * @param town The town to check. Cannot be null.
     * @return The town's authority, or 0.0 if the Town Authority system isn't enabled or the manager isn't available.
     * @throws NullPointerException if town is null.
     */
    public double getTownAuthority(Town town) {
        if (town == null) throw new NullPointerException("Town cannot be null");
        if (plugin.getTownAuthorityManager() == null) {
            return 0.0;
        }
        return plugin.getTownAuthorityManager().getAuthority(town);
    }

    /**
     * Gets the current government type of a town.
     *
     * @param town The town to check. Cannot be null.
     * @return The town's GovernmentType. Returns {@link GovernmentType#TRIBAL} if the town government manager isn't available.
     * @throws NullPointerException if town is null.
     */
    public GovernmentType getTownGovernmentType(Town town) {
        if (town == null) throw new NullPointerException("Town cannot be null");
        if (plugin.getTownGovManager() == null) {
            plugin.getLogger().warning("TownyPoliticsAPI: TownGovernmentManager is not available!");
            return GovernmentType.TRIBAL;
        }
        return plugin.getTownGovManager().getGovernmentType(town);
    }

    /**
     * Gets the current decadence level of a town.
     * Note: This depends on the town-level decadence system being enabled in TownyPolitics config.
     *
     * @param town The town to check. Cannot be null.
     * @return The town's decadence level (0.0 - 100.0), or 0.0 if the town decadence system isn't enabled or the manager isn't available.
     * @throws NullPointerException if town is null.
     */
    public double getTownDecadence(Town town) {
        if (town == null) throw new NullPointerException("Town cannot be null");
        if (plugin.getTownDecadenceManager() == null) {
            return 0.0;
        }
        return plugin.getTownDecadenceManager().getDecadence(town);
    }

    /**
     * Gets the set of active policies for a town.
     *
     * @param town The town to check. Cannot be null.
     * @return An unmodifiable set of {@link ActivePolicy} objects, or an empty set if none are active or the policy manager isn't available.
     * @throws NullPointerException if town is null.
     */
    public Set<ActivePolicy> getActiveTownPolicies(Town town) {
        if (town == null) throw new NullPointerException("Town cannot be null");
        if (plugin.getPolicyManager() == null) {
            plugin.getLogger().warning("TownyPoliticsAPI: PolicyManager is not available!");
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(plugin.getPolicyManager().getActivePolicies(town));
    }

    // --- Utility/General Methods ---

    /**
     * Retrieves a specific policy definition by its ID.
     *
     * @param policyId The unique identifier of the policy (e.g., "progressive_taxation"). Cannot be null or empty.
     * @return The {@link Policy} object, or null if no policy with that ID exists or the policy manager isn't available.
     * @throws NullPointerException if policyId is null.
     * @throws IllegalArgumentException if policyId is empty.
     */
    public Policy getPolicyDefinition(String policyId) {
        if (policyId == null) throw new NullPointerException("Policy ID cannot be null");
        if (policyId.isEmpty()) throw new IllegalArgumentException("Policy ID cannot be empty");
        if (plugin.getPolicyManager() == null) {
            plugin.getLogger().warning("TownyPoliticsAPI: PolicyManager is not available!");
            return null;
        }
        return plugin.getPolicyManager().getPolicy(policyId);
    }

    /**
     * Gets the main TownyPolitics plugin instance.
     * Use this carefully, prefer using dedicated API methods when possible.
     *
     * @return The TownyPolitics plugin instance.
     */
    public TownyPolitics getPlugin() {
        return plugin;
    }
}