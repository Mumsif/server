package com.glowslot.repository;

import com.glowslot.entity.Appointment;
import com.glowslot.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Appointment entities.
 * Handles queue management, atomic slot collision detection, and workstation state queries.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, String> {

    /**
     * Used for atomic slot concurrency check. Returns true if the slot is already taken.
     */
    boolean existsBySalonIdAndChairIdAndScheduledDateAndScheduledTimeAndStatusNot(
            String salonId, String chairId, String scheduledDate, String scheduledTime, AppointmentStatus status);

    /**
     * Find all active appointments for a specific salon and date (excluding CANCELLED).
     */
    List<Appointment> findBySalonIdAndScheduledDateAndStatusNot(
            String salonId, String scheduledDate, AppointmentStatus status);

    /**
     * Find all active appointments for a specific chair and date (excluding CANCELLED).
     */
    List<Appointment> findByChairIdAndScheduledDateAndStatusNot(
            String chairId, String scheduledDate, AppointmentStatus status);

    /**
     * Find queue items for a chair filtered by statuses (e.g. PENDING, SEATED, IN_SERVICE).
     */
    List<Appointment> findByChairIdAndStatusInOrderByScheduledTimeAsc(
            String chairId, List<AppointmentStatus> statuses);

    /**
     * Find queue items for a chair on a specific date, ordered by time.
     */
    List<Appointment> findByChairIdAndScheduledDateAndStatusInOrderByScheduledTimeAsc(
            String chairId, String scheduledDate, List<AppointmentStatus> statuses);

    /**
     * Find the currently seated customer in a chair.
     */
    Optional<Appointment> findByChairIdAndStatus(String chairId, AppointmentStatus status);

    /**
     * Count completed appointments on a date for telemetry metrics.
     */
    long countBySalonIdAndScheduledDateAndStatus(String salonId, String scheduledDate, AppointmentStatus status);

    /**
     * Find all appointments on a date for a salon.
     */
    List<Appointment> findBySalonIdAndScheduledDate(String salonId, String scheduledDate);

    /**
     * Count pending bookings ahead of a specific scheduled time on a date.
     */
    long countByChairIdAndScheduledDateAndScheduledTimeLessThanAndStatus(
            String chairId, String scheduledDate, String scheduledTime, AppointmentStatus status);
}


