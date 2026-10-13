package com.glowslot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the salon's wallet balance and per-cut commission fee.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletBalanceDTO {
    private double balanceLkr;
    private String currency;            // "LKR"
    private double commissionPerCutLkr; // 50.0
    private String lastUpdated;
}
