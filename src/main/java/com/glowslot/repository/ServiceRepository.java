package com.glowslot.repository;

import com.glowslot.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for ServiceEntity.
 * Fetches barber services offered by salons and parses multi-selected service IDs.
 */
@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, String> {

    /**
     * Find all services provided by a specific salon.
     */
    List<ServiceEntity> findBySalonId(String salonId);

    /**
     * Find services matching a list of selected service IDs.
     */
    List<ServiceEntity> findByIdIn(List<String> ids);
}
