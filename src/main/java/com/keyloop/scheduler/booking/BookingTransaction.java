package com.keyloop.scheduler.booking;

import com.keyloop.scheduler.catalog.Dealership;
import com.keyloop.scheduler.catalog.DealershipRepository;
import com.keyloop.scheduler.catalog.ServiceBay;
import com.keyloop.scheduler.catalog.ServiceBayRepository;
import com.keyloop.scheduler.catalog.ServiceType;
import com.keyloop.scheduler.catalog.ServiceTypeRepository;
import com.keyloop.scheduler.catalog.Technician;
import com.keyloop.scheduler.catalog.TechnicianRepository;
import com.keyloop.scheduler.common.error.ErrorCode;
import com.keyloop.scheduler.customer.Customer;
import com.keyloop.scheduler.customer.CustomerRepository;
import com.keyloop.scheduler.customer.Vehicle;
import com.keyloop.scheduler.customer.VehicleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One booking attempt in one transaction (design 04 §4). A separate bean so that
 * {@code BookingService} reaches it through the Spring proxy. Constraint violations
 * are deliberately not caught here; the caller decides whether to retry.
 */
@Component
public class BookingTransaction {

    private final AllocationQueries allocation;
    private final AppointmentRepository appointments;
    private final CustomerRepository customers;
    private final VehicleRepository vehicles;
    private final DealershipRepository dealerships;
    private final ServiceTypeRepository serviceTypes;
    private final TechnicianRepository technicians;
    private final ServiceBayRepository bays;
    private final Clock clock;

    public BookingTransaction(AllocationQueries allocation, AppointmentRepository appointments,
                              CustomerRepository customers, VehicleRepository vehicles,
                              DealershipRepository dealerships, ServiceTypeRepository serviceTypes,
                              TechnicianRepository technicians, ServiceBayRepository bays, Clock clock) {
        this.allocation = allocation;
        this.appointments = appointments;
        this.customers = customers;
        this.vehicles = vehicles;
        this.dealerships = dealerships;
        this.serviceTypes = serviceTypes;
        this.technicians = technicians;
        this.bays = bays;
        this.clock = clock;
    }

    @Transactional
    public Appointment tryAllocateAndInsert(BookingCommand cmd) {
        ServiceType serviceType = serviceTypes.findById(cmd.serviceTypeId()).orElseThrow();
        LocalDateTime start = cmd.startTime();
        LocalDateTime end = start.plusMinutes(serviceType.getDurationMinutes());

        UUID technicianId = allocation
                .bestTechnician(cmd.dealershipId(), serviceType.getRequiredSkill(), start, end)
                .orElseThrow(() -> new NoCapacityException(ErrorCode.NO_QUALIFIED_TECHNICIAN_AVAILABLE));
        UUID bayId = allocation
                .lowestFreeBay(cmd.dealershipId(), serviceType.getRequiredBayType(), start, end)
                .orElseThrow(() -> new NoCapacityException(ErrorCode.NO_SERVICE_BAY_AVAILABLE));

        Customer customer = customers.findById(cmd.customerId()).orElseThrow();
        Vehicle vehicle = vehicles.findById(cmd.vehicleId()).orElseThrow();
        Dealership dealership = dealerships.findById(cmd.dealershipId()).orElseThrow();
        Technician technician = technicians.findById(technicianId).orElseThrow();
        ServiceBay bay = bays.findById(bayId).orElseThrow();

        Appointment appointment = new Appointment(customer, vehicle, dealership, serviceType, technician, bay,
                start, end, cmd.idempotencyKey(), cmd.requestHash(), LocalDateTime.now(clock));
        return appointments.saveAndFlush(appointment);
    }
}
