package com.keyloop.scheduler.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "service_bay")
public class ServiceBay {

    @Id
    private UUID id;

    @Column(name = "dealership_id")
    private UUID dealershipId;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "bay_type")
    private BayType bayType;

    private boolean active;

    protected ServiceBay() {
    }

    public ServiceBay(UUID id, UUID dealershipId, String name, BayType bayType, boolean active) {
        this.id = id;
        this.dealershipId = dealershipId;
        this.name = name;
        this.bayType = bayType;
        this.active = active;
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

    public BayType getBayType() {
        return bayType;
    }

    public boolean isActive() {
        return active;
    }
}
