package com.keyloop.scheduler.catalog;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "technician")
public class Technician {

    @Id
    private UUID id;

    @Column(name = "dealership_id")
    private UUID dealershipId;

    private String name;

    private boolean active;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "technician_skill", joinColumns = @JoinColumn(name = "technician_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "skill")
    private Set<Skill> skills = new HashSet<>();

    protected Technician() {
    }

    public Technician(UUID id, UUID dealershipId, String name, boolean active, Set<Skill> skills) {
        this.id = id;
        this.dealershipId = dealershipId;
        this.name = name;
        this.active = active;
        this.skills = new HashSet<>(skills);
    }

    public UUID getId() {
        return id;
    }

    public UUID getDealershipId() {
        return dealershipId;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public Set<Skill> getSkills() {
        return Collections.unmodifiableSet(skills);
    }
}
