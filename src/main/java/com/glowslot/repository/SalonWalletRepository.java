package com.glowslot.repository;

import com.glowslot.entity.SalonWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository for SalonWallet entities.
 * Manages balance deductions and prepaid credit balance for salons.
 */
@Repository
public interface SalonWalletRepository extends JpaRepository<SalonWallet, String> {
}
