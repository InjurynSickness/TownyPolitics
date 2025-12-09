package com.orbismc.townyPolitics.vassalage;

import java.util.UUID;

/**
 * Represents a vassalage relationship between two nations
 */
public class VassalageRelationship {
    private final UUID liegeUUID;
    private final UUID vassalUUID;
    private final long establishedTime;
    private double tributeRate; // 0.0 to 0.10 (0% to 10%)
    private long lastTributeTime;

    public VassalageRelationship(UUID liegeUUID, UUID vassalUUID, double tributeRate) {
        this.liegeUUID = liegeUUID;
        this.vassalUUID = vassalUUID;
        this.tributeRate = Math.max(0.0, Math.min(0.10, tributeRate)); // Clamp to 0-10%
        this.establishedTime = System.currentTimeMillis();
        this.lastTributeTime = System.currentTimeMillis();
    }

    public VassalageRelationship(UUID liegeUUID, UUID vassalUUID, double tributeRate, 
                               long establishedTime, long lastTributeTime) {
        this.liegeUUID = liegeUUID;
        this.vassalUUID = vassalUUID;
        this.tributeRate = Math.max(0.0, Math.min(0.10, tributeRate));
        this.establishedTime = establishedTime;
        this.lastTributeTime = lastTributeTime;
    }

    // Getters
    public UUID getLiegeUUID() { return liegeUUID; }
    public UUID getVassalUUID() { return vassalUUID; }
    public double getTributeRate() { return tributeRate; }
    public long getEstablishedTime() { return establishedTime; }
    public long getLastTributeTime() { return lastTributeTime; }

    // Setters
    public void setTributeRate(double rate) {
        this.tributeRate = Math.max(0.0, Math.min(0.10, rate));
    }

    public void setLastTributeTime(long time) {
        this.lastTributeTime = time;
    }

    /**
     * Get the age of this vassalage relationship in days
     */
    public long getAgeInDays() {
        return (System.currentTimeMillis() - establishedTime) / (24 * 60 * 60 * 1000);
    }

    /**
     * Check if tribute is due (daily)
     */
    public boolean isTributeDue() {
        long daysSinceLastTribute = (System.currentTimeMillis() - lastTributeTime) / (24 * 60 * 60 * 1000);
        return daysSinceLastTribute >= 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VassalageRelationship)) return false;
        VassalageRelationship other = (VassalageRelationship) obj;
        return liegeUUID.equals(other.liegeUUID) && vassalUUID.equals(other.vassalUUID);
    }

    @Override
    public int hashCode() {
        return liegeUUID.hashCode() ^ vassalUUID.hashCode();
    }

    @Override
    public String toString() {
        return "VassalageRelationship{liege=" + liegeUUID + ", vassal=" + vassalUUID + 
               ", tribute=" + (tributeRate * 100) + "%, age=" + getAgeInDays() + " days}";
    }
}