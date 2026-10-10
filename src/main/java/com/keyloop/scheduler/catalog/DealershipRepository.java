package com.keyloop.scheduler.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DealershipRepository extends JpaRepository<Dealership, UUID> {
}
