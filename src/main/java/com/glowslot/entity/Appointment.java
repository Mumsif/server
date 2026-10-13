package com.glowslot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * =========================================================================================
 * CORE STUDY FILE: Appointment (JPA Entity)
 * =========================================================================================
 * PURPOSE:
 * The primary database record representing a salon visit in Trimly (online or walk-in).
 * Maps to the "appointments" table in the H2 SQL database.
 *
 * KEY FIELDS & THEIR ROLES:
 * - ticketNumber:          Human-readable identifier ("GS-1042" for online, "WI-105" for walk-ins).
 * - chairId:               The assigned styling station ("chair_1" / "chair_2").
 * - scheduledTime:         The booked 45-minute slot time (e.g. "10:30").
 * - estimatedChairTime:    Dynamically updated time if a +15 min delay shift occurs.
 * - otp:                   4-digit security code ("4821") verified by the barber before seating.
 * - status:                State lifecycle:
 *                          PENDING -> SEATED -> IN_SERVICE -> COMPLETED (or CANCELLED / NO_SHOW).
 * - startedAt / completedAt: Timestamps used to drive the barber's live countdown timer.
 * =========================================================================================
 */
@Entity
@Table(name = "appointments", indexes = {
    @Index(name = "idx_appt_date_slot", columnList = "scheduledDate, scheduledTime"),
    @Index(name = "idx_appt_chair_status", columnList = "chairId, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @Column(length = 64)
    private String id; // Unique ID, e.g. "app_1728551234"

    @Column(nullable = false, length = 16)
    private String ticketNumber; // e.g. "GS-1042" (online) or "WI-105" (walk-in)

    @Column(nullable = false, length = 64)
    private String salonId; // e.g. "salon_1"

    @Column(nullable = false, length = 64)
    private String chairId; // "chair_1" or "chair_2"

    @Column(nullable = false, length = 128)
    private String customerName;

    @Column(nullable = false, length = 32)
    private String customerPhone;

    @Column(nullable = false, length = 10)
    private String scheduledDate; // Format: "YYYY-MM-DD"

    @Column(nullable = false, length = 5)
    private String scheduledTime; // Format: "10:30"

    @Column(length = 10)
    private String estimatedChairTime; // Shifted time if delay occurred, e.g. "10:45"

    private int totalDurationMinutes; // e.g. 45

    private double totalPriceLkr; // e.g. 800.0

    @Column(nullable = false, length = 4)
    private String otp; // 4-digit code presented upon arrival (e.g. "4821")

    private int queuePosition; // Real-time position in chair queue

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status; // PENDING, SEATED, IN_SERVICE, COMPLETED, CANCELLED, NO_SHOW

    @Column(columnDefinition = "TEXT")
    private String selectedServiceIdsJson; // Comma-separated or JSON list of service IDs

    private LocalDateTime createdAt;

    private LocalDateTime startedAt; // Recorded when barber validates OTP and customer is seated

    private LocalDateTime completedAt; // Recorded when barber clicks "Complete Service"
}
