package com.keyloop.scheduler.catalog;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DealershipRepository extends JpaRepository<Dealership, UUID> {

    @EntityGraph(attributePaths = "openingHours")
    @Query("select d from Dealership d order by d.name asc")
    List<Dealership> findAllWithOpeningHoursOrderByName();
}
