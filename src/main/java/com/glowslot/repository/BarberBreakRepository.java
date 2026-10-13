package com.glowslot.repository;

import com.glowslot.entity.BarberBreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for BarberBreak entities.
 * Used for querying scheduled barber breaks to prevent customer slot booking collisions.
 */
@Repository
public interface BarberBreakRepository extends JpaRepository<BarberBreak, String> {

    /**
     * Find all breaks scheduled on a given date across all chairs.
     */
    List<BarberBreak> findByBreakDate(String breakDate);

    /**
     * Find all breaks scheduled for a specific chair on a given date.
     */
    List<BarberBreak> findByChairIdAndBreakDate(String chairId, String breakDate);

    /**
     * Check if a specific slot is already blocked by a break.
     */
    boolean existsByChairIdAndBreakDateAndTimeSlot(String chairId, String breakDate, String timeSlot);

    /**
     * Remove break by chair ID and time slot.
     */
    void deleteByChairIdAndBreakDateAndTimeSlot(String chairId, String breakDate, String timeSlot);

    /**
     * Find breaks by chair ID.
     */
    List<BarberBreak> findByChairId(String chairId);
}
