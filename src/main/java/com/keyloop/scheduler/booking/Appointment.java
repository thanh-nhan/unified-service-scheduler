package com.keyloop.scheduler.booking;

import com.keyloop.scheduler.catalog.Dealership;
import com.keyloop.scheduler.catalog.ServiceBay;
import com.keyloop.scheduler.catalog.ServiceType;
import com.keyloop.scheduler.catalog.Technician;
import com.keyloop.scheduler.customer.Customer;
import com.keyloop.scheduler.customer.Vehicle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The generated {@code time_range} column is deliberately not mapped; PostgreSQL computes it.
 * No {@code @Version}: cancellation uses a conditional update.
 */
@Entity
@Table(name = "appointment")
public class Appointment {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dealership_id")
    private Dealership dealership;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_type_id")
    private ServiceType serviceType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technician_id")
    private Technician technician;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_bay_id")
    private ServiceBay serviceBay;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    protected Appointment() {
    }

    public Appointment(Customer customer, Vehicle vehicle, Dealership dealership, ServiceType serviceType,
                       Technician technician, ServiceBay serviceBay, LocalDateTime startTime,
                       LocalDateTime endTime, String idempotencyKey, String requestHash,
                       LocalDateTime createdAt) {
        this.id = UUID.randomUUID();
        this.customer = customer;
        this.vehicle = vehicle;
        this.dealership = dealership;
        this.serviceType = serviceType;
        this.technician = technician;
        this.serviceBay = serviceBay;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AppointmentStatus.CONFIRMED;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Dealership getDealership() {
        return dealership;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public Technician getTechnician() {
        return technician;
    }

    public ServiceBay getServiceBay() {
        return serviceBay;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
}
