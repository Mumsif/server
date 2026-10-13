package com.glowslot.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object sent by the Barber Workstation for fast walk-in customer registration.
 * Generates an immediate ticket (e.g. WI-105) and seats or queues the customer directly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalkInRequestDTO {

    @NotBlank(message = "Chair ID is required")
    private String chairId;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    /**
     * Service identifier or code (e.g. "QUICK_CUT", "BEARD_TRIM", "COMBO_EXPRESS").
     */
    private String serviceType;

    /**
     * Duration in minutes (defaults to 45 mins).
     */
    private Integer customDurationMinutes;

    /**
     * Price in LKR (e.g. 400.0, 800.0).
     */
    private Double priceLkr;
}
