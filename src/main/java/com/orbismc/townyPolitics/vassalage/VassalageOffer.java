package com.orbismc.townyPolitics.vassalage;

import java.util.UUID;

/**
 * Represents a pending vassalage offer
 */
public class VassalageOffer {
    private final UUID offerUUID;
    private final UUID liegeUUID;
    private final UUID targetUUID;
    private final double proposedTributeRate;
    private final long offerTime;
    private final long expiryTime;

    public VassalageOffer(UUID liegeUUID, UUID targetUUID, double proposedTributeRate, long expiryDurationMs) {
        this.offerUUID = UUID.randomUUID();
        this.liegeUUID = liegeUUID;
        this.targetUUID = targetUUID;
        this.proposedTributeRate = Math.max(0.0, Math.min(0.10, proposedTributeRate));
        this.offerTime = System.currentTimeMillis();
        this.expiryTime = this.offerTime + expiryDurationMs;
    }

    public VassalageOffer(UUID offerUUID, UUID liegeUUID, UUID targetUUID, double proposedTributeRate, 
                         long offerTime, long expiryTime) {
        this.offerUUID = offerUUID;
        this.liegeUUID = liegeUUID;
        this.targetUUID = targetUUID;
        this.proposedTributeRate = proposedTributeRate;
        this.offerTime = offerTime;
        this.expiryTime = expiryTime;
    }

    // Getters
    public UUID getOfferUUID() { return offerUUID; }
    public UUID getLiegeUUID() { return liegeUUID; }
    public UUID getTargetUUID() { return targetUUID; }
    public double getProposedTributeRate() { return proposedTributeRate; }
    public long getOfferTime() { return offerTime; }
    public long getExpiryTime() { return expiryTime; }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }

    public long getTimeRemaining() {
        return Math.max(0, expiryTime - System.currentTimeMillis());
    }

    public String formatTimeRemaining() {
        long remaining = getTimeRemaining();
        if (remaining <= 0) return "Expired";

        long hours = remaining / (60 * 60 * 1000);
        remaining %= (60 * 60 * 1000);
        long minutes = remaining / (60 * 1000);

        if (hours > 0) {
            return hours + " hours, " + minutes + " minutes";
        } else {
            return minutes + " minutes";
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VassalageOffer)) return false;
        VassalageOffer other = (VassalageOffer) obj;
        return offerUUID.equals(other.offerUUID);
    }

    @Override
    public int hashCode() {
        return offerUUID.hashCode();
    }
}