package com.orbismc.townyPolitics.managers;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.policy.Policy;
import com.orbismc.townyPolitics.policy.PolicyEffects;
import com.orbismc.townyPolitics.policy.ActivePolicy;
import com.orbismc.townyPolitics.storage.IPolicyStorage;
import com.orbismc.townyPolitics.utils.DelegateLogger;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PolicyManager implements Manager {
    private final TownyPolitics plugin;
    private final IPolicyStorage storage;
    private final DelegateLogger logger;
    private final Map<String, Policy> availablePolicies;
    private final Map<UUID, Set<ActivePolicy>> activeTownPolicies;
    private final Map<UUID, Set<ActivePolicy>> activeNationPolicies;
    private final Map<UUID, Long> lastPolicyActionTimes;
    private final long policyCooldown;

    public PolicyManager(TownyPolitics plugin, IPolicyStorage storage, GovernmentManager govManager, TownGovernmentManager townGovManager) {
        this.plugin = plugin;
        this.storage = storage;
        this.logger = new DelegateLogger(plugin, "PolicyManager");
        this.availablePolicies = new HashMap<>();
        this.activeTownPolicies = new ConcurrentHashMap<>();
        this.activeNationPolicies = new ConcurrentHashMap<>();
        this.lastPolicyActionTimes = new ConcurrentHashMap<>();

        // Get policy cooldown from config (default: 3 days)
        this.policyCooldown = plugin.getConfig().getLong("policies.cooldown_days", 3) * 24L * 60L * 60L * 1000L;

        // Load policies from config
        loadPolicies();

        // Load active policies from storage
        loadActivePolicies();
    }

    /**
     * Reload policy data
     */
    public void reload() {
        // Reload policies and active policies
        loadPolicies();
        loadActivePolicies();
        logger.info("Policy data reloaded");
    }

    /**
     * Load all policy definitions from configuration
     */
    public void loadPolicies() {
        availablePolicies.clear();

        // Load built-in policies from policies.yml
        File policiesFile = new File(plugin.getDataFolder(), "policies.yml");
        if (policiesFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(policiesFile);
            loadPoliciesFromConfig(config);
        } else {
            logger.warning("policies.yml file not found. Creating default...");
            plugin.saveResource("policies.yml", false);
            policiesFile = new File(plugin.getDataFolder(), "policies.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(policiesFile);
            loadPoliciesFromConfig(config);
        }

        // Load town policies from town_policies.yml if it exists
        File townPoliciesFile = new File(plugin.getDataFolder(), "town_policies.yml");
        if (townPoliciesFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(townPoliciesFile);
            loadPoliciesFromConfig(config);
        } else {
            logger.info("town_policies.yml file not found. Town policies will use defaults from policies.yml.");
        }

        // Load custom policies from main config
        loadPoliciesFromConfig(plugin.getConfig());

        logger.info("Loaded " + availablePolicies.size() + " policies");
    }

    /**
     * Load policy definitions from a configuration section
     */
    private void loadPoliciesFromConfig(FileConfiguration config) {
        ConfigurationSection policiesSection = config.getConfigurationSection("policies.available");
        if (policiesSection == null) {
            logger.warning("No 'policies.available' section found in config");
            return;
        }

        for (String policyId : policiesSection.getKeys(false)) {
            try {
                ConfigurationSection policySection = policiesSection.getConfigurationSection(policyId);
                if (policySection == null) continue;

                String name = policySection.getString("name", policyId);
                String description = policySection.getString("description", "");
                double cost = policySection.getDouble("cost", 10.0);
                int duration = policySection.getInt("duration", -1);
                Policy.PolicyType type = Policy.PolicyType.valueOf(
                        policySection.getString("type", "ECONOMIC").toUpperCase());
                boolean townOnly = policySection.getBoolean("town_only", false);

                // Load allowed governments
                Set<GovernmentType> allowedGovernments = new HashSet<>();
                List<String> govList = policySection.getStringList("allowed_governments");
                for (String gov : govList) {
                    try {
                        allowedGovernments.add(GovernmentType.valueOf(gov.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        logger.warning("Invalid government type: " + gov + " for policy: " + policyId);
                    }
                }

                double minPoliticalPower = policySection.getDouble("min_political_power", 0.0);
                double maxCorruption = policySection.getDouble("max_corruption", 100.0);

                // Load effects
                ConfigurationSection effectsSection = policySection.getConfigurationSection("effects");
                PolicyEffects.Builder effectsBuilder = new PolicyEffects.Builder();

                if (effectsSection != null) {
                    effectsBuilder
                            .taxModifier(effectsSection.getDouble("tax", 1.0))
                            .tradeModifier(effectsSection.getDouble("trade", 1.0))
                            .economyModifier(effectsSection.getDouble("economy", 1.0))
                            .authorityGainModifier(effectsSection.getDouble("political_power_gain", 1.0))
                            .decadenceGainModifier(effectsSection.getDouble("corruption_gain", 1.0))
                            .maxAuthorityModifier(effectsSection.getDouble("max_political_power", 1.0))
                            .resourceOutputModifier(effectsSection.getDouble("resource_output", 1.0))
                            .spendingModifier(effectsSection.getDouble("spending", 1.0));

                    // Town-specific effects
                    if (effectsSection.contains("plot_cost")) {
                        effectsBuilder.plotCostModifier(effectsSection.getDouble("plot_cost", 1.0));
                    }
                    if (effectsSection.contains("plot_tax")) {
                        effectsBuilder.plotTaxModifier(effectsSection.getDouble("plot_tax", 1.0));
                    }
                    if (effectsSection.contains("resident_capacity")) {
                        effectsBuilder.residentCapacityModifier(effectsSection.getDouble("resident_capacity", 1.0));
                    }
                    if (effectsSection.contains("upkeep")) {
                        effectsBuilder.upkeepModifier(effectsSection.getDouble("upkeep", 1.0));
                    }
                    if (effectsSection.contains("town_block_cost")) {
                        effectsBuilder.townBlockCostModifier(effectsSection.getDouble("town_block_cost", 1.0));
                    }
                    if (effectsSection.contains("town_block_bonus")) {
                        effectsBuilder.townBlockBonusModifier(effectsSection.getDouble("town_block_bonus", 1.0));
                    }
                }

                Policy policy = new Policy(policyId, name, description, cost, duration, type,
                        allowedGovernments, minPoliticalPower, maxCorruption, townOnly, effectsBuilder.build());

                availablePolicies.put(policyId, policy);
                logger.fine("Loaded policy: " + policyId);

            } catch (Exception e) {
                logger.severe("Error loading policy '" + policyId + "': " + e.getMessage());
            }
        }
    }

    /**
     * Load active policies from storage
     */
    public void loadActivePolicies() {
        activeTownPolicies.clear();
        activeNationPolicies.clear();

        // Load from storage
        Map<UUID, Set<ActivePolicy>> townPolicies = storage.loadActivePolicies(false);
        Map<UUID, Set<ActivePolicy>> nationPolicies = storage.loadActivePolicies(true);

        // Process and filter out expired policies
        for (Map.Entry<UUID, Set<ActivePolicy>> entry : townPolicies.entrySet()) {
            Set<ActivePolicy> validPolicies = entry.getValue().stream()
                    .filter(policy -> !policy.isExpired())
                    .collect(Collectors.toSet());

            if (!validPolicies.isEmpty()) {
                activeTownPolicies.put(entry.getKey(), validPolicies);
            }
        }

        for (Map.Entry<UUID, Set<ActivePolicy>> entry : nationPolicies.entrySet()) {
            Set<ActivePolicy> validPolicies = entry.getValue().stream()
                    .filter(policy -> !policy.isExpired())
                    .collect(Collectors.toSet());

            if (!validPolicies.isEmpty()) {
                activeNationPolicies.put(entry.getKey(), validPolicies);
            }
        }

        logger.info("Loaded " + activeNationPolicies.size() + " nations with active policies");
        logger.info("Loaded " + activeTownPolicies.size() + " towns with active policies");
    }

    @Override
    public void loadData() {
        loadPolicies();
        loadActivePolicies();
    }

    @Override
    public void saveAllData() {
        storage.saveAll();
        logger.info("Saved policy data to storage");
    }

    /**
     * Get all available policies
     */
    public Collection<Policy> getAvailablePolicies() {
        return availablePolicies.values();
    }

    /**
     * Get all town-specific policies
     */
    public Collection<Policy> getAvailableTownPolicies() {
        return availablePolicies.values().stream()
                .filter(policy -> policy.isTownOnly() || policy.getEffects().hasTownEffects())
                .collect(Collectors.toList());
    }

    /**
     * Get all nation-specific policies
     */
    public Collection<Policy> getAvailableNationPolicies() {
        return availablePolicies.values().stream()
                .filter(policy -> !policy.isTownOnly())
                .collect(Collectors.toList());
    }

    /**
     * Get a policy by its ID
     */
    public Policy getPolicy(String policyId) {
        return availablePolicies.get(policyId);
    }

    /**
     * Check if a policy exists
     */
    public boolean policyExists(String policyId) {
        return availablePolicies.containsKey(policyId);
    }

    /**
     * Get active policies for a town
     */
    public Set<ActivePolicy> getActivePolicies(Town town) {
        return activeTownPolicies.getOrDefault(town.getUUID(), new HashSet<>());
    }

    /**
     * Get active policies for a nation
     */
    public Set<ActivePolicy> getActivePolicies(Nation nation) {
        return activeNationPolicies.getOrDefault(nation.getUUID(), new HashSet<>());
    }

    /**
     * Check if a town has the policy active
     */
    public boolean hasActivePolicy(Town town, String policyId) {
        Set<ActivePolicy> policies = getActivePolicies(town);
        return policies.stream().anyMatch(p -> p.getPolicyId().equals(policyId));
    }

    /**
     * Check if a nation has the policy active
     */
    public boolean hasActivePolicy(Nation nation, String policyId) {
        Set<ActivePolicy> policies = getActivePolicies(nation);
        return policies.stream().anyMatch(p -> p.getPolicyId().equals(policyId));
    }

    /**
     * Check if entity is on cooldown for policy changes
     */
    public boolean isOnCooldown(UUID entityId) {
        long lastActionTime = lastPolicyActionTimes.getOrDefault(entityId, 0L);
        return System.currentTimeMillis() - lastActionTime < policyCooldown;
    }

    /**
     * Get remaining cooldown time
     */
    public long getCooldownTimeRemaining(UUID entityId) {
        if (!isOnCooldown(entityId)) {
            return 0;
        }

        long lastActionTime = lastPolicyActionTimes.getOrDefault(entityId, 0L);
        return policyCooldown - (System.currentTimeMillis() - lastActionTime);
    }

    /**
     * Format cooldown time remaining
     */
    public String formatCooldownTime(UUID entityId) {
        long millis = getCooldownTimeRemaining(entityId);

        long days = millis / (24 * 60 * 60 * 1000);
        millis %= (24 * 60 * 60 * 1000);

        long hours = millis / (60 * 60 * 1000);
        millis %= (60 * 60 * 1000);

        long minutes = millis / (60 * 1000);

        if (days > 0) {
            return days + " days, " + hours + " hours";
        } else if (hours > 0) {
            return hours + " hours, " + minutes + " minutes";
        } else {
            return minutes + " minutes";
        }
    }

    /**
     * Enact a policy for a town
     * @return true if successful, false if failed
     */
    public boolean enactPolicy(Town town, String policyId) {
        if (!policyExists(policyId)) {
            logger.warning("Attempted to enact non-existent policy: " + policyId);
            return false;
        }

        if (isOnCooldown(town.getUUID())) {
            logger.fine("Town " + town.getName() + " is on policy cooldown");
            return false;
        }

        Policy policy = getPolicy(policyId);

        // Check if the policy is town-compatible
        if (!policy.isTownOnly() && !policy.getEffects().hasTownEffects() && !policy.getType().equals(Policy.PolicyType.ECONOMIC)) {
            logger.fine("Policy " + policyId + " is not compatible with towns");
            return false;
        }

        // Check if town already has this policy
        if (hasActivePolicy(town, policyId)) {
            logger.fine("Town " + town.getName() + " already has policy: " + policyId);
            return false;
        }

        // Check if town meets policy requirements
        TownGovernmentManager townGovManager = plugin.getTownGovManager();
        GovernmentType townGovType = townGovManager.getGovernmentType(town);

        if (!policy.isGovernmentAllowed(townGovType)) {
            logger.fine("Government type " + townGovType + " not allowed for policy: " + policyId);
            return false;
        }

        // Check corruption level if town corruption system is enabled
        if (plugin.getTownDecadenceManager() != null) {
            double corruption = plugin.getTownDecadenceManager().getCorruption(town);
            if (corruption > policy.getMaxCorruption()) {
                logger.fine("Town " + town.getName() + " has too much corruption for policy: " + policyId);
                return false;
            }
        }

        // Get town political power manager
        TownAuthorityManager townPpManager = plugin.getTownAuthorityManager();
        if (townPpManager != null) {
            // Check if town has enough political power to afford the policy
            double currentPP = townPpManager.getAuthority(town);
            if (currentPP < policy.getCost()) {
                logger.fine("Town " + town.getName() + " cannot afford policy " + policyId +
                        " (cost: " + policy.getCost() + ", available: " + currentPP + ")");
                return false;
            }

            // Deduct political power
            townPpManager.removeAuthority(town, policy.getCost());
            logger.info("Deducted " + policy.getCost() + " political power from town " +
                    town.getName() + " for policy: " + policy.getName());
        } else {
            // If town political power system isn't enabled, allow the policy without cost
            logger.warning("Town political power system not enabled, allowing policy without cost");
        }

        // Create active policy
        ActivePolicy activePolicy = new ActivePolicy(policyId, town.getUUID(), false, policy.getDuration());

        // Add to active policies
        Set<ActivePolicy> townPolicies = activeTownPolicies.getOrDefault(town.getUUID(), new HashSet<>());
        townPolicies.add(activePolicy);
        activeTownPolicies.put(town.getUUID(), townPolicies);

        // Save to storage
        storage.saveActivePolicy(activePolicy);

        // Set cooldown
        lastPolicyActionTimes.put(town.getUUID(), System.currentTimeMillis());

        logger.info("Town " + town.getName() + " enacted policy: " + policy.getName());
        return true;
    }

    /**
     * Enact a policy for a nation
     * @return true if successful, false if failed
     */
    public boolean enactPolicy(Nation nation, String policyId) {
        if (!policyExists(policyId)) {
            logger.warning("Attempted to enact non-existent policy: " + policyId);
            return false;
        }

        if (isOnCooldown(nation.getUUID())) {
            logger.fine("Nation " + nation.getName() + " is on policy cooldown");
            return false;
        }

        Policy policy = getPolicy(policyId);

        // Check if policy is town-only
        if (policy.isTownOnly()) {
            logger.fine("Policy " + policyId + " is town-only and cannot be enacted by nations");
            return false;
        }

        // Check if nation already has this policy
        if (hasActivePolicy(nation, policyId)) {
            logger.fine("Nation " + nation.getName() + " already has policy: " + policyId);
            return false;
        }

        // Check if nation meets policy requirements
        GovernmentManager govManager = plugin.getGovManager();
        GovernmentType nationGovType = govManager.getGovernmentType(nation);

        if (!policy.isGovernmentAllowed(nationGovType)) {
            logger.fine("Government type " + nationGovType + " not allowed for policy: " + policyId);
            return false;
        }

        // Additional checks (political power, corruption, etc.)
        AuthorityManager ppManager = plugin.getAuthorityManager();
        double currentPP = ppManager.getPoliticalPower(nation);

        if (currentPP < policy.getMinPoliticalPower()) {
            logger.fine("Nation " + nation.getName() + " doesn't have enough political power for policy: " + policyId);
            return false;
        }

        DecadenceManager decadenceManager = plugin.getDecadenceManager();
        double corruption = decadenceManager.getDecadence(nation);

        if (corruption > policy.getMaxCorruption()) {
            logger.fine("Nation " + nation.getName() + " has too much corruption for policy: " + policyId);
            return false;
        }

        // Check if nation can afford the policy
        if (currentPP < policy.getCost()) {
            logger.fine("Nation " + nation.getName() + " cannot afford policy: " + policyId);
            return false;
        }

        // Deduct political power
        ppManager.removePoliticalPower(nation, policy.getCost());

        // Create active policy
        ActivePolicy activePolicy = new ActivePolicy(policyId, nation.getUUID(), true, policy.getDuration());

        // Add to active policies
        Set<ActivePolicy> nationPolicies = activeNationPolicies.getOrDefault(nation.getUUID(), new HashSet<>());
        nationPolicies.add(activePolicy);
        activeNationPolicies.put(nation.getUUID(), nationPolicies);

        // Save to storage
        storage.saveActivePolicy(activePolicy);

        // Set cooldown
        lastPolicyActionTimes.put(nation.getUUID(), System.currentTimeMillis());

        logger.info("Nation " + nation.getName() + " enacted policy: " + policy.getName());
        return true;
    }

    /**
     * Revoke an active policy
     * @return true if successful, false if failed
     */
    public boolean revokePolicy(UUID entityId, UUID policyId, boolean isNation) {
        Map<UUID, Set<ActivePolicy>> policyMap = isNation ? activeNationPolicies : activeTownPolicies;

        if (!policyMap.containsKey(entityId)) {
            return false;
        }

        if (isOnCooldown(entityId)) {
            logger.fine("Entity " + entityId + " is on policy cooldown");
            return false;
        }

        Set<ActivePolicy> policies = policyMap.get(entityId);
        Optional<ActivePolicy> policyOpt = policies.stream()
                .filter(p -> p.getId().equals(policyId))
                .findFirst();

        if (policyOpt.isEmpty()) {
            return false;
        }

        ActivePolicy policy = policyOpt.get();

        // Remove from active policies
        policies.remove(policy);
        if (policies.isEmpty()) {
            policyMap.remove(entityId);
        } else {
            policyMap.put(entityId, policies);
        }

        // Remove from storage
        storage.removeActivePolicy(policy.getId());

        // Set cooldown
        lastPolicyActionTimes.put(entityId, System.currentTimeMillis());

        logger.info((isNation ? "Nation" : "Town") + " " + entityId + " revoked policy: " + policy.getPolicyId());
        return true;
    }

    /**
     * Process daily updates for policies
     */
    public void processNewDay() {
        logger.info("Processing daily policy updates");

        // Process town policies
        for (UUID townId : new HashSet<>(activeTownPolicies.keySet())) {
            Set<ActivePolicy> policies = activeTownPolicies.get(townId);
            Set<ActivePolicy> expiredPolicies = policies.stream()
                    .filter(ActivePolicy::isExpired)
                    .collect(Collectors.toSet());

            if (!expiredPolicies.isEmpty()) {
                // Remove expired policies
                policies.removeAll(expiredPolicies);

                if (policies.isEmpty()) {
                    activeTownPolicies.remove(townId);
                } else {
                    activeTownPolicies.put(townId, policies);
                }

                // Remove from storage
                for (ActivePolicy policy : expiredPolicies) {
                    storage.removeActivePolicy(policy.getId());
                    logger.info("Town policy expired: " + policy.getPolicyId());
                }
            }
        }

        // Process nation policies
        for (UUID nationId : new HashSet<>(activeNationPolicies.keySet())) {
            Set<ActivePolicy> policies = activeNationPolicies.get(nationId);
            Set<ActivePolicy> expiredPolicies = policies.stream()
                    .filter(ActivePolicy::isExpired)
                    .collect(Collectors.toSet());

            if (!expiredPolicies.isEmpty()) {
                // Remove expired policies
                policies.removeAll(expiredPolicies);

                if (policies.isEmpty()) {
                    activeNationPolicies.remove(nationId);
                } else {
                    activeNationPolicies.put(nationId, policies);
                }

                // Remove from storage
                for (ActivePolicy policy : expiredPolicies) {
                    storage.removeActivePolicy(policy.getId());
                    logger.info("Nation policy expired: " + policy.getPolicyId());
                }
            }
        }
    }

    /**
     * Get the combined effects of all active policies for a town
     */
    public PolicyEffects getCombinedPolicyEffects(Town town) {
        Set<ActivePolicy> policies = getActivePolicies(town);

        // Base values with no modifiers (1.0 = no effect)
        double taxModifier = 1.0;
        double tradeModifier = 1.0;
        double economyModifier = 1.0;
        double politicalPowerGainModifier = 1.0;
        double corruptionGainModifier = 1.0;
        double maxPoliticalPowerModifier = 1.0;
        double resourceOutputModifier = 1.0;
        double spendingModifier = 1.0;
        double plotCostModifier = 1.0;
        double plotTaxModifier = 1.0;
        double residentCapacityModifier = 1.0;
        double upkeepModifier = 1.0;
        double townBlockCostModifier = 1.0;
        double townBlockBonusModifier = 1.0;

        // Apply each policy's effects
        for (ActivePolicy activePolicy : policies) {
            Policy policy = getPolicy(activePolicy.getPolicyId());
            if (policy == null) continue;

            PolicyEffects effects = policy.getEffects();

            // Multiply modifiers
            taxModifier *= effects.getTaxModifier();
            tradeModifier *= effects.getTradeModifier();
            economyModifier *= effects.getEconomyModifier();
            politicalPowerGainModifier *= effects.getAuthorityGainModifier();
            corruptionGainModifier *= effects.getDecadenceGainModifier();
            maxPoliticalPowerModifier *= effects.getMaxAuthorityModifier();
            resourceOutputModifier *= effects.getResourceOutputModifier();
            spendingModifier *= effects.getSpendingModifier();

            // Town-specific modifiers
            plotCostModifier *= effects.getPlotCostModifier();
            plotTaxModifier *= effects.getPlotTaxModifier();
            residentCapacityModifier *= effects.getResidentCapacityModifier();
            upkeepModifier *= effects.getUpkeepModifier();
            townBlockCostModifier *= effects.getTownBlockCostModifier();
            townBlockBonusModifier *= effects.getTownBlockBonusModifier();
        }

        // Build and return combined effects
        return new PolicyEffects.Builder()
                .taxModifier(taxModifier)
                .tradeModifier(tradeModifier)
                .economyModifier(economyModifier)
                .authorityGainModifier(politicalPowerGainModifier)
                .decadenceGainModifier(corruptionGainModifier)
                .maxAuthorityModifier(maxPoliticalPowerModifier)
                .resourceOutputModifier(resourceOutputModifier)
                .spendingModifier(spendingModifier)
                .plotCostModifier(plotCostModifier)
                .plotTaxModifier(plotTaxModifier)
                .residentCapacityModifier(residentCapacityModifier)
                .upkeepModifier(upkeepModifier)
                .townBlockCostModifier(townBlockCostModifier)
                .townBlockBonusModifier(townBlockBonusModifier)
                .build();
    }

    /**
     * Get the combined effects of all active policies for a nation
     */
    public PolicyEffects getCombinedPolicyEffects(Nation nation) {
        Set<ActivePolicy> policies = getActivePolicies(nation);

        // Base values with no modifiers
        double taxModifier = 1.0;
        double tradeModifier = 1.0;
        double economyModifier = 1.0;
        double politicalPowerGainModifier = 1.0;
        double corruptionGainModifier = 1.0;
        double maxPoliticalPowerModifier = 1.0;
        double resourceOutputModifier = 1.0;
        double spendingModifier = 1.0;

        // Apply each policy's effects
        for (ActivePolicy activePolicy : policies) {
            Policy policy = getPolicy(activePolicy.getPolicyId());
            if (policy == null) continue;

            PolicyEffects effects = policy.getEffects();

            // Multiply modifiers
            taxModifier *= effects.getTaxModifier();
            tradeModifier *= effects.getTradeModifier();
            economyModifier *= effects.getEconomyModifier();
            politicalPowerGainModifier *= effects.getAuthorityGainModifier();
            corruptionGainModifier *= effects.getDecadenceGainModifier();
            maxPoliticalPowerModifier *= effects.getMaxAuthorityModifier();
            resourceOutputModifier *= effects.getResourceOutputModifier();
            spendingModifier *= effects.getSpendingModifier();
        }

        // Build and return combined effects
        return new PolicyEffects.Builder()
                .taxModifier(taxModifier)
                .tradeModifier(tradeModifier)
                .economyModifier(economyModifier)
                .authorityGainModifier(politicalPowerGainModifier)
                .decadenceGainModifier(corruptionGainModifier)
                .maxAuthorityModifier(maxPoliticalPowerModifier)
                .resourceOutputModifier(resourceOutputModifier)
                .spendingModifier(spendingModifier)
                .build();
    }
}