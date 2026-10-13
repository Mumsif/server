package com.glowslot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the customer currently seated in a chair with active timer telemetry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveInChairDTO {
    private String appointmentId;
    private String ticketNumber;
    private String customerName;
    private String serviceNames;
    private String startedAt;
    private int totalDurationMinutes;
    private int remainingMinutes;
    private int remainingSeconds;
}
