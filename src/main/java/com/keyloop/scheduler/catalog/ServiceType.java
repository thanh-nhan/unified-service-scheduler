package com.keyloop.scheduler.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "service_type")
public class ServiceType {

    @Id
    private UUID id;

    private String code;

    private String name;

    @Column(name = "duration_minutes")
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_skill")
    private Skill requiredSkill;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_bay_type")
    private BayType requiredBayType;

    protected ServiceType() {
    }

    public ServiceType(UUID id, String code, String name, int durationMinutes,
                       Skill requiredSkill, BayType requiredBayType) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.durationMinutes = durationMinutes;
        this.requiredSkill = requiredSkill;
        this.requiredBayType = requiredBayType;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public Skill getRequiredSkill() {
        return requiredSkill;
    }

    public BayType getRequiredBayType() {
        return requiredBayType;
    }
}
