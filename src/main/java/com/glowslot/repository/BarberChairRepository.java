package com.glowslot.repository;

import com.glowslot.entity.BarberChair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for BarberChair entities.
 */
@Repository
public interface BarberChairRepository extends JpaRepository<BarberChair, String> {

    /**
     * Retrieve all chairs operating under a specific salon, sorted by chair number (1, 2, ...).
     */
    List<BarberChair> findBySalonIdOrderByChairNumberAsc(String salonId);
}
