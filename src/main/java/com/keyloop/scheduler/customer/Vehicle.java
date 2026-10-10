package com.keyloop.scheduler.customer;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "vehicle")
public class Vehicle {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    private String vin;

    private String make;

    private String model;

    protected Vehicle() {
    }

    public Vehicle(UUID id, Customer customer, String vin, String make, String model) {
        this.id = id;
        this.customer = customer;
        this.vin = vin;
        this.make = make;
        this.model = model;
    }

    public UUID getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getVin() {
        return vin;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public boolean belongsTo(UUID customerId) {
        return customer.getId().equals(customerId);
    }
}
