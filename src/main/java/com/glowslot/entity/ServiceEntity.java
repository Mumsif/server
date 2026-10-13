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
 * Entity representing a barber service offered in the salon catalog.
 * Maps to database table "services".
 */
@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceEntity {

    @Id
    @Column(length = 64)
    private String id; // e.g. "srv_1"

    @Column(nullable = false, length = 64)
    private String salonId; // "salon_1"

    @Column(nullable = false, length = 128)
    private String name; // e.g. "Normal Haircut with Beard Cut"

    @Column(length = 255)
    private String description;

    private int durationMinutes; // e.g. 45

    private double priceLkr; // e.g. 800.0

    @Column(length = 64)
    private String category; // "HAIRCUT", "BEARD", "COMBO", "FACIAL"
}
