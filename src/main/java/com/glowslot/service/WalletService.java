package com.glowslot.service;

import com.glowslot.dto.response.WalletBalanceDTO;
import com.glowslot.entity.SalonWallet;
import com.glowslot.repository.SalonWalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * =========================================================================================
 * CORE STUDY FILE: WalletService
 * =========================================================================================
 * PURPOSE:
 * Manages the salon's prepaid wallet, tracking revenue and deducting Trimly's platform fee.
 *
 * HOW IT WORKS (MONETIZATION MODEL):
 * 1. PREPAID BALANCE:
 *    Each salon maintains a prepaid balance in Sri Lankan Rupees (LKR).
 * 2. AUTOMATIC COMMISSION DEDUCTION:
 *    Every time a haircut/service is completed (triggered by QueueManagementService.completeService),
 *    this service automatically deducts LKR 50.0 from the balance.
 * 3. REAL-TIME TELEMETRY:
 *    The remaining balance is displayed on the barber workstation dashboard so salon owners
 *    always know their account status.
 * =========================================================================================
 */
@Slf4j
@Service
public class WalletService {

    private final SalonWalletRepository walletRepository;

    public WalletService(SalonWalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public static final double DEFAULT_COMMISSION_LKR = 50.0;

    /**
     * Fetch the current prepaid balance for a salon.
     */
    @Transactional(readOnly = true)
    public WalletBalanceDTO getWalletBalance(String salonId) {
        SalonWallet wallet = walletRepository.findById(salonId)
                .orElseGet(() -> SalonWallet.builder()
                        .salonId(salonId)
                        .balanceLkr(2450.0)
                        .currency("LKR")
                        .commissionPerCutLkr(DEFAULT_COMMISSION_LKR)
                        .lastUpdated(LocalDateTime.now())
                        .build());

        return WalletBalanceDTO.builder()
                .balanceLkr(wallet.getBalanceLkr())
                .currency(wallet.getCurrency() != null ? wallet.getCurrency() : "LKR")
                .commissionPerCutLkr(wallet.getCommissionPerCutLkr())
                .lastUpdated(wallet.getLastUpdated() != null ? wallet.getLastUpdated().toString() : LocalDateTime.now().toString())
                .build();
    }

    /**
     * Deducts commission (default LKR 50.0) from the salon's prepaid wallet upon service completion.
     */
    @Transactional
    public double deductCommission(String salonId, double amount) {
        SalonWallet wallet = walletRepository.findById(salonId)
                .orElseGet(() -> {
                    SalonWallet created = SalonWallet.builder()
                            .salonId(salonId)
                            .balanceLkr(2450.0)
                            .currency("LKR")
                            .commissionPerCutLkr(DEFAULT_COMMISSION_LKR)
                            .lastUpdated(LocalDateTime.now())
                            .build();
                    return walletRepository.save(created);
                });

        double currentBalance = wallet.getBalanceLkr();
        double newBalance = Math.max(0.0, currentBalance - amount);
        wallet.setBalanceLkr(newBalance);
        wallet.setLastUpdated(LocalDateTime.now());
        walletRepository.save(wallet);

        log.info("Deducted LKR {} commission for salon {}. New balance: LKR {}", amount, salonId, newBalance);
        return amount;
    }
}
