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
 * Entity representing the salon's prepaid balance wallet.
 * Every time a haircut appointment is marked COMPLETED,
 * Trimly deducts LKR 50 platform commission from this wallet.
 * Maps to database table "salon_wallets".
 */
@Entity
@Table(name = "salon_wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalonWallet {

    @Id
    @Column(length = 64)
    private String salonId; // e.g. "salon_1"

    private double balanceLkr; // e.g. 2450.0

    @Column(length = 10)
    private String currency; // "LKR"

    private double commissionPerCutLkr; // 50.0

    private LocalDateTime lastUpdated;
}
