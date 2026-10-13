package com.glowslot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Composite DTO representing the complete live state of the Barber Workstation.
 * Includes all chairs (active cut + upcoming queue), operational stats, wallet balance, and breaks.
 * Returned by GET /api/v1/barber/workstation/{salonId}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkstationStateDTO {
    private List<BarberStationChairDTO> chairs;
    private SalonTelemetryStatsDTO stats;
    private WalletBalanceDTO wallet;
    private List<BarberBreakDTO> breaks;
}
