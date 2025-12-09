package com.orbismc.townyPolitics.policy;

import com.orbismc.townyPolitics.government.GovernmentType;
import java.util.HashSet;
import java.util.Set;

public class PolicyBuilder {
    private String id;
    private String name;
    private String description;
    private double cost;
    private int duration = -1; // Default: permanent
    private Policy.PolicyType type = Policy.PolicyType.ECONOMIC; // Default
    private Set<GovernmentType> allowedGovernments = new HashSet<>();
    private double minPoliticalPower = 0.0;
    private double maxCorruption = 100.0;
    private boolean townOnly = false;
    private PolicyEffects.Builder effectsBuilder = new PolicyEffects.Builder();

    public PolicyBuilder id(String id) {
        this.id = id;
        return this;
    }

    public PolicyBuilder name(String name) {
        this.name = name;
        return this;
    }

    public PolicyBuilder description(String description) {
        this.description = description;
        return this;
    }

    public PolicyBuilder cost(double cost) {
        this.cost = cost;
        return this;
    }

    public PolicyBuilder duration(int duration) {
        this.duration = duration;
        return this;
    }

    public PolicyBuilder type(Policy.PolicyType type) {
        this.type = type;
        return this;
    }

    public PolicyBuilder allowGovernment(GovernmentType government) {
        this.allowedGovernments.add(government);
        return this;
    }

    public PolicyBuilder allowGovernments(Set<GovernmentType> governments) {
        this.allowedGovernments.addAll(governments);
        return this;
    }

    public PolicyBuilder minPoliticalPower(double minPP) {
        this.minPoliticalPower = minPP;
        return this;
    }

    public PolicyBuilder maxCorruption(double maxCorruption) {
        this.maxCorruption = maxCorruption;
        return this;
    }

    public PolicyBuilder townOnly(boolean townOnly) {
        this.townOnly = townOnly;
        return this;
    }

    public PolicyBuilder effects(PolicyEffects.Builder effectsBuilder) {
        this.effectsBuilder = effectsBuilder;
        return this;
    }

    public Policy build() {
        return new Policy(id, name, description, cost, duration, type,
                allowedGovernments, minPoliticalPower, maxCorruption, townOnly, effectsBuilder.build());
    }
}