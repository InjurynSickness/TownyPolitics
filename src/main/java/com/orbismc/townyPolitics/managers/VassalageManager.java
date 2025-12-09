package com.orbismc.townyPolitics.managers;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.storage.IVassalageStorage;
import com.orbismc.townyPolitics.utils.DelegateLogger;
import com.orbismc.townyPolitics.vassalage.VassalageOffer;
import com.orbismc.townyPolitics.vassalage.VassalageRelationship;
import com.palmergames.bukkit.towny.object.Nation;

import java.util.*;
import java.util.stream.Collectors;

public class VassalageManager implements Manager {
    private final TownyPolitics plugin;
    private final IVassalageStorage storage;
    private final DelegateLogger logger;

    // Configuration values
    private final double minLiegeAuthority;
    private final double authorityMaintenancePerVassal;
    private final double vassalBreakCost;
    private final long offerExpiryTime;
    private final long breakawayGracePeriod;
    private final double maxTributeRate;

    // Cache
    private Map<UUID, Set<VassalageRelationship>> liegeToVassals;
    private Map<UUID, VassalageRelationship> vassalToLiege;
    private Map<UUID, VassalageOffer> pendingOffers;

    public VassalageManager(TownyPolitics plugin, IVassalageStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        this.logger = new DelegateLogger(plugin, "VassalageManager");

        // Load configuration
        this.minLiegeAuthority = plugin.getConfig().getDouble("vassalage.min_liege_authority", 50.0);
        this.authorityMaintenancePerVassal = plugin.getConfig().getDouble("vassalage.authority_maintenance_per_vassal", 2.0);
        this.vassalBreakCost = plugin.getConfig().getDouble("vassalage.vassal_break_cost", 25.0);
        this.offerExpiryTime = plugin.getConfig().getLong("vassalage.offer_expiry_hours", 24) * 60 * 60 * 1000;
        this.breakawayGracePeriod = plugin.getConfig().getLong("vassalage.breakaway_grace_period_days", 7) * 24 * 60 * 60 * 1000;
        this.maxTributeRate = plugin.getConfig().getDouble("vassalage.max_tribute_rate", 0.10);

        // Initialize caches
        this.liegeToVassals = new HashMap<>();
        this.vassalToLiege = new HashMap<>();
        this.pendingOffers = new HashMap<>();

        loadData();
    }

    @Override
    public void loadData() {
        // Clear caches
        liegeToVassals.clear();
        vassalToLiege.clear();
        pendingOffers.clear();

        // Load relationships
        List<VassalageRelationship> relationships = storage.getAllRelationships();
        for (VassalageRelationship relationship : relationships) {
            // Update liege -> vassals mapping
            liegeToVassals.computeIfAbsent(relationship.getLiegeUUID(), k -> new HashSet<>())
                    .add(relationship);
            
            // Update vassal -> liege mapping
            vassalToLiege.put(relationship.getVassalUUID(), relationship);
        }

        // Load offers
        List<VassalageOffer> offers = storage.getAllOffers();
        for (VassalageOffer offer : offers) {
            if (!offer.isExpired()) {
                pendingOffers.put(offer.getOfferUUID(), offer);
            }
        }

        // Cleanup expired offers
        storage.cleanupExpiredOffers();

        logger.info("Loaded " + relationships.size() + " vassalage relationships and " + 
                   pendingOffers.size() + " pending offers");
    }

    @Override
    public void saveAllData() {
        storage.saveAll();
        logger.info("Saved vassalage data to storage");
    }

    // Relationship Management
    public boolean canFormVassalage(Nation liege, Nation vassal) {
        // Check if they're the same nation
        if (liege.getUUID().equals(vassal.getUUID())) {
            return false;
        }
        
        // Check if either nation is already in a vassalage relationship
        // Vassals cannot have vassals, and nations can't be both liege and vassal
        if (isVassal(vassal) || isLiege(vassal)) {
            return false;
        }
        
        // Check if liege has sufficient authority to maintain a vassal
        double liegeAuthority = plugin.getAuthorityManager().getAuthority(liege);
        if (liegeAuthority < minLiegeAuthority) {
            return false;
        }
        
        // Check government type restrictions
        GovernmentType liegeGov = plugin.getGovManager().getGovernmentType(liege);
        GovernmentType vassalGov = plugin.getGovManager().getGovernmentType(vassal);
        
        // Tribal governments cannot participate in vassalage (too decentralized)
        // Theocracies cannot participate in vassalage (they serve a higher divine power)
        if (liegeGov == GovernmentType.TRIBAL || liegeGov == GovernmentType.THEOCRACY ||
            vassalGov == GovernmentType.TRIBAL || vassalGov == GovernmentType.THEOCRACY) {
            return false;
        }
        
        return true;
    }

    public VassalageOffer createOffer(Nation liege, Nation target, double tributeRate) {
        if (!canFormVassalage(liege, target)) {
            return null;
        }

        // Remove any existing offers from this liege to this target
        removeOffersFromTo(liege.getUUID(), target.getUUID());

        // Create new offer
        VassalageOffer offer = new VassalageOffer(liege.getUUID(), target.getUUID(), 
                                                 tributeRate, offerExpiryTime);
        
        // Save to storage and cache
        storage.saveOffer(offer);
        pendingOffers.put(offer.getOfferUUID(), offer);

        logger.info("Created vassalage offer from " + liege.getName() + " to " + target.getName() + 
                   " with " + (tributeRate * 100) + "% tribute rate");

        return offer;
    }
    
    public boolean acceptOffer(UUID offerUUID, Nation acceptingNation) {
        VassalageOffer offer = pendingOffers.get(offerUUID);
        if (offer == null || offer.isExpired()) {
            return false;
        }

        if (!offer.getTargetUUID().equals(acceptingNation.getUUID())) {
            return false;
        }

        Nation liege = plugin.getTownyAPI().getNation(offer.getLiegeUUID());
        if (liege == null) {
            removeOffer(offerUUID);
            return false;
        }

        // Final validation
        if (!canFormVassalage(liege, acceptingNation)) {
            removeOffer(offerUUID);
            return false;
        }

        // Create the relationship
        VassalageRelationship relationship = new VassalageRelationship(
                offer.getLiegeUUID(), 
                offer.getTargetUUID(), 
                offer.getProposedTributeRate()
        );

        // Save relationship
        storage.saveRelationship(relationship);
        
        // Update caches
        liegeToVassals.computeIfAbsent(relationship.getLiegeUUID(), k -> new HashSet<>())
                .add(relationship);
        vassalToLiege.put(relationship.getVassalUUID(), relationship);

        // Remove the offer and any other offers to/from these nations
        removeOffer(offerUUID);
        removeOffersFromTo(liege.getUUID(), acceptingNation.getUUID());
        removeOffersFromTo(acceptingNation.getUUID(), liege.getUUID());

        logger.info("Vassalage established: " + liege.getName() + " (liege) -> " + 
                   acceptingNation.getName() + " (vassal) with " + 
                   (relationship.getTributeRate() * 100) + "% tribute");

        return true;
    }

    public boolean releaseVassal(Nation liege, Nation vassal) {
        VassalageRelationship relationship = getRelationship(liege.getUUID(), vassal.getUUID());
        if (relationship == null) {
            return false;
        }

        // Remove the relationship
        removeRelationship(liege.getUUID(), vassal.getUUID());

        logger.info("Liege " + liege.getName() + " released vassal " + vassal.getName());
        return true;
    }

    public boolean breakVassalage(Nation vassal) {
        VassalageRelationship relationship = vassalToLiege.get(vassal.getUUID());
        if (relationship == null) {
            return false;
        }

        // Check if vassal can afford the break cost
        double currentAuthority = plugin.getAuthorityManager().getAuthority(vassal);
        if (currentAuthority < vassalBreakCost) {
            return false;
        }

        // Deduct authority cost
        plugin.getAuthorityManager().removeAuthority(vassal, vassalBreakCost);

        // Remove the relationship
        removeRelationship(relationship.getLiegeUUID(), vassal.getUUID());

        Nation liege = plugin.getTownyAPI().getNation(relationship.getLiegeUUID());
        String liegeName = liege != null ? liege.getName() : "Unknown";

        logger.info("Vassal " + vassal.getName() + " broke free from " + liegeName + 
                   " for " + vassalBreakCost + " authority");

        return true;
    }

    public boolean setTributeRate(Nation liege, Nation vassal, double newRate) {
        VassalageRelationship relationship = getRelationship(liege.getUUID(), vassal.getUUID());
        if (relationship == null) {
            return false;
        }

        // Clamp tribute rate
        newRate = Math.max(0.0, Math.min(maxTributeRate, newRate));

        relationship.setTributeRate(newRate);
        storage.saveRelationship(relationship);

        logger.info("Updated tribute rate for " + vassal.getName() + " to " + (newRate * 100) + "%");
        return true;
    }

    // Query Methods
    public boolean isLiege(Nation nation) {
        return liegeToVassals.containsKey(nation.getUUID()) && 
               !liegeToVassals.get(nation.getUUID()).isEmpty();
    }

    public boolean isVassal(Nation nation) {
        return vassalToLiege.containsKey(nation.getUUID());
    }

    public List<VassalageRelationship> getVassals(Nation liege) {
        Set<VassalageRelationship> vassals = liegeToVassals.get(liege.getUUID());
        return vassals != null ? new ArrayList<>(vassals) : new ArrayList<>();
    }

    public VassalageRelationship getLiege(Nation vassal) {
        return vassalToLiege.get(vassal.getUUID());
    }

    public VassalageRelationship getRelationship(UUID liegeUUID, UUID vassalUUID) {
        Set<VassalageRelationship> vassals = liegeToVassals.get(liegeUUID);
        if (vassals != null) {
            return vassals.stream()
                    .filter(r -> r.getVassalUUID().equals(vassalUUID))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    public List<VassalageOffer> getOffersTo(Nation nation) {
        return pendingOffers.values().stream()
                .filter(offer -> offer.getTargetUUID().equals(nation.getUUID()) && !offer.isExpired())
                .collect(Collectors.toList());
    }

    public List<VassalageOffer> getOffersFrom(Nation nation) {
        return pendingOffers.values().stream()
                .filter(offer -> offer.getLiegeUUID().equals(nation.getUUID()) && !offer.isExpired())
                .collect(Collectors.toList());
    }

    // Authority and Economic Methods
    public double calculateAuthorityMaintenance(Nation liege) {
        List<VassalageRelationship> vassals = getVassals(liege);
        return vassals.size() * authorityMaintenancePerVassal;
    }

    public double calculateTotalTributeIncome(Nation liege) {
        // This would need to be called during daily processing with actual income data
        // For now, return 0 as tribute is calculated when actual income is processed
        return 0.0;
    }

    public boolean canAffordMaintenance(Nation liege) {
        double maintenance = calculateAuthorityMaintenance(liege);
        double currentAuthority = plugin.getAuthorityManager().getAuthority(liege);
        return currentAuthority >= maintenance;
    }

    // Daily Processing
    public void processNewDay() {
        logger.info("Processing daily vassalage maintenance and tribute");

        // Process all lieges
        for (UUID liegeUUID : new HashSet<>(liegeToVassals.keySet())) {
            Nation liege = plugin.getTownyAPI().getNation(liegeUUID);
            if (liege == null) {
                // Nation no longer exists, remove all relationships
                removeAllRelationshipsForNation(liegeUUID);
                continue;
            }

            processLiegeMaintenance(liege);
        }

        // Cleanup expired offers
        cleanupExpiredOffers();
    }

    private void processLiegeMaintenance(Nation liege) {
        double maintenance = calculateAuthorityMaintenance(liege);
        if (maintenance <= 0) {
            return;
        }

        double currentAuthority = plugin.getAuthorityManager().getAuthority(liege);
        
        if (currentAuthority >= maintenance) {
            // Liege can afford maintenance
            plugin.getAuthorityManager().removeAuthority(liege, maintenance);
            logger.fine("Liege " + liege.getName() + " paid " + maintenance + " authority maintenance");
        } else {
            // Liege cannot afford maintenance - release all vassals
            List<VassalageRelationship> vassals = getVassals(liege);
            for (VassalageRelationship relationship : vassals) {
                Nation vassal = plugin.getTownyAPI().getNation(relationship.getVassalUUID());
                String vassalName = vassal != null ? vassal.getName() : "Unknown";
                
                removeRelationship(liege.getUUID(), relationship.getVassalUUID());
                logger.info("Released vassal " + vassalName + " due to " + liege.getName() + 
                           " unable to pay maintenance");
            }
        }
    }

    // Utility Methods - Made public to fix compilation errors
    public void removeRelationship(UUID liegeUUID, UUID vassalUUID) {
        // Remove from storage
        storage.removeRelationship(liegeUUID, vassalUUID);
        
        // Update caches
        Set<VassalageRelationship> vassals = liegeToVassals.get(liegeUUID);
        if (vassals != null) {
            vassals.removeIf(r -> r.getVassalUUID().equals(vassalUUID));
            if (vassals.isEmpty()) {
                liegeToVassals.remove(liegeUUID);
            }
        }
        
        vassalToLiege.remove(vassalUUID);
    }

    private void removeOffer(UUID offerUUID) {
        storage.removeOffer(offerUUID);
        pendingOffers.remove(offerUUID);
    }

    private void removeOffersFromTo(UUID fromUUID, UUID toUUID) {
        List<VassalageOffer> toRemove = pendingOffers.values().stream()
                .filter(offer -> offer.getLiegeUUID().equals(fromUUID) && offer.getTargetUUID().equals(toUUID))
                .collect(Collectors.toList());
        
        for (VassalageOffer offer : toRemove) {
            removeOffer(offer.getOfferUUID());
        }
    }

    public void removeAllRelationshipsForNation(UUID nationUUID) {
        // Remove as liege
        Set<VassalageRelationship> vassals = liegeToVassals.get(nationUUID);
        if (vassals != null) {
            for (VassalageRelationship relationship : new HashSet<>(vassals)) {
                removeRelationship(nationUUID, relationship.getVassalUUID());
            }
        }
        
        // Remove as vassal
        VassalageRelationship relationship = vassalToLiege.get(nationUUID);
        if (relationship != null) {
            removeRelationship(relationship.getLiegeUUID(), nationUUID);
        }
        
        // Remove all offers from/to this nation
        storage.removeOffersFrom(nationUUID);
        storage.removeOffersTo(nationUUID);
        
        pendingOffers.entrySet().removeIf(entry -> 
                entry.getValue().getLiegeUUID().equals(nationUUID) || 
                entry.getValue().getTargetUUID().equals(nationUUID));
    }

    private void cleanupExpiredOffers() {
        List<UUID> expiredOffers = pendingOffers.values().stream()
                .filter(VassalageOffer::isExpired)
                .map(VassalageOffer::getOfferUUID)
                .collect(Collectors.toList());
        
        for (UUID offerUUID : expiredOffers) {
            removeOffer(offerUUID);
        }
        
        if (!expiredOffers.isEmpty()) {
            logger.info("Cleaned up " + expiredOffers.size() + " expired vassalage offers");
        }
    }

    // Configuration getters
    public double getMinLiegeAuthority() { return minLiegeAuthority; }
    public double getAuthorityMaintenancePerVassal() { return authorityMaintenancePerVassal; }
    public double getVassalBreakCost() { return vassalBreakCost; }
    public double getMaxTributeRate() { return maxTributeRate; }

    // Storage access method for external updates
    public void saveRelationship(VassalageRelationship relationship) {
        storage.saveRelationship(relationship);
    }
}