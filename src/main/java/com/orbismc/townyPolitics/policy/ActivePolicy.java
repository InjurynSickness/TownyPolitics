package com.orbismc.townyPolitics.policy;

import java.util.UUID;

public class ActivePolicy {
    private final UUID id;
    private final String policyId;
    private final UUID entityId;
    private final boolean isNation; // true for nation, false for town
    private final long enactedTime;
    private final long expiryTime; // -1 for permanent

    public ActivePolicy(UUID id, String policyId, UUID entityId, boolean isNation, long enactedTime, long expiryTime) {
        this.id = id;
        this.policyId = policyId;
        this.entityId = entityId;
        this.isNation = isNation;
        this.enactedTime = enactedTime;
        this.expiryTime = expiryTime;
    }

    public ActivePolicy(String policyId, UUID entityId, boolean isNation, int durationDays) {
        this.id = UUID.randomUUID();
        this.policyId = policyId;
        this.entityId = entityId;
        this.isNation = isNation;
        this.enactedTime = System.currentTimeMillis();

        if (durationDays < 0) {
            this.expiryTime = -1; // Permanent policy
        } else {
            this.expiryTime = this.enactedTime + (durationDays * 24L * 60L * 60L * 1000L);
        }
    }

    // Getters
    public UUID getId() { return id; }
    public String getPolicyId() { return policyId; }
    public UUID getEntityId() { return entityId; }
    public boolean isNation() { return isNation; }
    public long getEnactedTime() { return enactedTime; }
    public long getExpiryTime() { return expiryTime; }

    public boolean isExpired() {
        if (expiryTime == -1) {
            return false; // Permanent policy never expires
        }
        return System.currentTimeMillis() > expiryTime;
    }

    public long getRemainingTime() {
        if (expiryTime == -1) {
            return -1; // Permanent policy
        }

        long remaining = expiryTime - System.currentTimeMillis();
        return Math.max(0, remaining); // Don't return negative values
    }

    public String formatRemainingTime() {
        if (expiryTime == -1) {
            return "Permanent";
        }

        long remaining = getRemainingTime();
        if (remaining <= 0) {
            return "Expired";
        }

        // Convert to days, hours, minutes
        long days = remaining / (24 * 60 * 60 * 1000);
        remaining %= (24 * 60 * 60 * 1000);

        long hours = remaining / (60 * 60 * 1000);
        remaining %= (60 * 60 * 1000);

        long minutes = remaining / (60 * 1000);

        if (days > 0) {
            return days + " days, " + hours + " hours";
        } else if (hours > 0) {
            return hours + " hours, " + minutes + " minutes";
        } else {
            return minutes + " minutes";
        }
    }
}