package com.keyloop.scheduler.catalog;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "dealership")
public class Dealership {

    @Id
    private UUID id;

    private String name;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealership_id", insertable = false, updatable = false)
    private List<OpeningHours> openingHours = new ArrayList<>();

    protected Dealership() {
    }

    public Dealership(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<OpeningHours> getOpeningHours() {
        return Collections.unmodifiableList(openingHours);
    }
}
