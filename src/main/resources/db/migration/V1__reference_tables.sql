CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE dealership (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL
);

CREATE TABLE opening_hours (
    dealership_id UUID NOT NULL,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    opens_at TIME NOT NULL,
    closes_at TIME NOT NULL,
    PRIMARY KEY (dealership_id, day_of_week),
    CONSTRAINT ck_opening_hours_closes_after_opens CHECK (closes_at > opens_at)
);

CREATE TABLE service_type (
    id UUID PRIMARY KEY,
    code VARCHAR NOT NULL UNIQUE,
    name VARCHAR NOT NULL,
    duration_minutes INT NOT NULL CHECK (duration_minutes > 0 AND duration_minutes % 15 = 0),
    required_skill VARCHAR NOT NULL,
    required_bay_type VARCHAR NOT NULL
);

CREATE TABLE technician (
    id UUID PRIMARY KEY,
    dealership_id UUID NOT NULL,
    name VARCHAR NOT NULL,
    active BOOLEAN NOT NULL,
    UNIQUE (id, dealership_id)
);

CREATE TABLE technician_skill (
    technician_id UUID NOT NULL,
    skill VARCHAR NOT NULL,
    PRIMARY KEY (technician_id, skill)
);

CREATE TABLE service_bay (
    id UUID PRIMARY KEY,
    dealership_id UUID NOT NULL,
    name VARCHAR NOT NULL,
    bay_type VARCHAR NOT NULL,
    active BOOLEAN NOT NULL,
    UNIQUE (id, dealership_id)
);

CREATE TABLE customer (
    id UUID PRIMARY KEY,
    name VARCHAR NOT NULL,
    email VARCHAR NOT NULL UNIQUE,
    phone VARCHAR
);

CREATE TABLE vehicle (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    vin VARCHAR(17) NOT NULL UNIQUE,
    make VARCHAR NOT NULL,
    model VARCHAR NOT NULL,
    UNIQUE (id, customer_id)
);
