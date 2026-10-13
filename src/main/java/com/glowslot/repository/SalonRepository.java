package com.glowslot.repository;

import com.glowslot.entity.Salon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository for Salon entities.
 * Automatically provides CRUD and pagination operations for salons.
 */
@Repository
public interface SalonRepository extends JpaRepository<Salon, String> {
}
