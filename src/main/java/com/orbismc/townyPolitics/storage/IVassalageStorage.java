package com.orbismc.townyPolitics.storage;

import com.orbismc.townyPolitics.vassalage.VassalageRelationship;
import com.orbismc.townyPolitics.vassalage.VassalageOffer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface for Vassalage storage implementations
 */
public interface IVassalageStorage {
    // Relationships
    void saveRelationship(VassalageRelationship relationship);
    void removeRelationship(UUID liegeUUID, UUID vassalUUID);
    Optional<VassalageRelationship> getRelationship(UUID liegeUUID, UUID vassalUUID);
    List<VassalageRelationship> getVassalsOf(UUID liegeUUID);
    Optional<VassalageRelationship> getLiegeOf(UUID vassalUUID);
    List<VassalageRelationship> getAllRelationships();

    // Offers
    void saveOffer(VassalageOffer offer);
    void removeOffer(UUID offerUUID);
    void removeOffersFrom(UUID liegeUUID);
    void removeOffersTo(UUID targetUUID);
    Optional<VassalageOffer> getOffer(UUID offerUUID);
    List<VassalageOffer> getOffersFrom(UUID liegeUUID);
    List<VassalageOffer> getOffersTo(UUID targetUUID);
    List<VassalageOffer> getAllOffers();
    void cleanupExpiredOffers();

    void saveAll();
}