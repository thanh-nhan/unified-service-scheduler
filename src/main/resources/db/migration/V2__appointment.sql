CREATE TABLE appointment (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    dealership_id UUID NOT NULL REFERENCES dealership (id),
    service_type_id UUID NOT NULL REFERENCES service_type (id),
    technician_id UUID NOT NULL,
    service_bay_id UUID NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    time_range TSRANGE GENERATED ALWAYS AS (tsrange(start_time, end_time, '[)')) STORED,
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    cancelled_at TIMESTAMP,

    CONSTRAINT ck_appointment_time_order CHECK (end_time > start_time),
    CONSTRAINT ck_appointment_status CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    CONSTRAINT ck_appointment_cancelled_at CHECK ((status = 'CANCELLED') = (cancelled_at IS NOT NULL)),

    CONSTRAINT fk_appointment_technician FOREIGN KEY (technician_id, dealership_id)
        REFERENCES technician (id, dealership_id),
    CONSTRAINT fk_appointment_bay FOREIGN KEY (service_bay_id, dealership_id)
        REFERENCES service_bay (id, dealership_id),
    CONSTRAINT fk_appointment_vehicle FOREIGN KEY (vehicle_id, customer_id)
        REFERENCES vehicle (id, customer_id),

    CONSTRAINT uq_appointment_idempotency UNIQUE (customer_id, idempotency_key),

    CONSTRAINT ex_appointment_technician_overlap
        EXCLUDE USING gist (technician_id WITH =, time_range WITH &&) WHERE (status = 'CONFIRMED'),
    CONSTRAINT ex_appointment_bay_overlap
        EXCLUDE USING gist (service_bay_id WITH =, time_range WITH &&) WHERE (status = 'CONFIRMED'),
    CONSTRAINT ex_appointment_vehicle_overlap
        EXCLUDE USING gist (vehicle_id WITH =, time_range WITH &&) WHERE (status = 'CONFIRMED')
);

CREATE INDEX ix_appointment_dealership_start ON appointment (dealership_id, start_time);
CREATE INDEX ix_appointment_customer_start ON appointment (customer_id, start_time, id);
