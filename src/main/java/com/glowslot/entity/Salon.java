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
 * Entity representing a barber shop / salon profile.
 * Maps to database table "salons".
 */
@Entity
@Table(name = "salons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Salon {

    @Id
    @Column(length = 64)
    private String id; // e.g. "salon_1"

    @Column(nullable = false, length = 128)
    private String name; // e.g. "Western Cutters"

    @Column(nullable = false, length = 255)
    private String address; // e.g. "Main Street, Akkaraipattu"

    @Column(length = 32)
    private String phone; // e.g. "+94 77 123 4567"

    @Column(length = 500)
    private String imageUrl;

    private double rating; // e.g. 4.9

    private int reviewCount; // e.g. 128

    @Column(length = 10)
    private String openingTime; // "08:15"

    @Column(length = 10)
    private String closingTime; // "22:30"
}
