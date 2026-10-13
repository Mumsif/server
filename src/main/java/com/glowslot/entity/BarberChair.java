package com.glowslot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing an individual styling chair/workstation inside a salon.
 * For example:
 * - Chair 1: Dhanu
 * - Chair 2: Thambi
 * Maps to database table "barber_chairs".
 */
@Entity
@Table(name = "barber_chairs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarberChair {

    @Id
    @Column(length = 64)
    private String id; // e.g. "chair_1"

    @Column(nullable = false, length = 64)
    private String salonId; // "salon_1"

    @Column(nullable = false)
    private int chairNumber; // 1, 2

    @Column(nullable = false, length = 100)
    private String barberName; // e.g. "Dhanu"

    @Column(length = 500)
    private String barberAvatar;

    @Column(length = 255)
    private String specialization; // e.g. "Fade Master & Beard Sculpting"

    @Column(nullable = false, length = 32)
    private String status; // "ONLINE", "BUSY", "ON_BREAK"

    @Column(length = 64)
    private String currentAppointmentId; // ID of seated customer if any
}
