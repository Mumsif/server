package com.glowslot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing live operational metrics on the salon workstation dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonTelemetryStatsDTO {
    private double dailyRevenueLkr;
    private int completedAppointmentsCount;
    private int avgTurnaroundMinutes;
    private int queueVelocityScore;
    private int activeChairsCount;
    private int totalChairsCount;
}
