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

import java.time.LocalDateTime;

/**
 * Entity representing a scheduled break taken by a barber (e.g. Lunch, Tea).
 * When a break is scheduled for a given chair, date, and slot, customer booking
 * is automatically blocked for that slot.
 * Maps to database table "barber_breaks".
 */
@Entity
@Table(name = "barber_breaks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarberBreak {

    @Id
    @Column(length = 64)
    private String id; // e.g. "break_1728551234"

    @Column(nullable = false, length = 64)
    private String chairId; // "chair_1" or "chair_2"

    @Column(length = 100)
    private String barberName; // "Dhanu"

    @Column(nullable = false, length = 10)
    private String breakDate; // "YYYY-MM-DD"

    @Column(nullable = false, length = 5)
    private String timeSlot; // "13:30"

    @Column(length = 100)
    private String reason; // "Lunch Break", "Tea Break"

    private LocalDateTime createdAt;
}
