package com.keyloop.scheduler.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceTypeRepository extends JpaRepository<ServiceType, UUID> {

    List<ServiceType> findAllByOrderByNameAsc();
}
