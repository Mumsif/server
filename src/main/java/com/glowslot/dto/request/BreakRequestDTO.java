package com.glowslot.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object sent when a barber schedules a break (e.g. Lunch, Tea).
 * Immediately blocks that 45-minute slot from customer booking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakRequestDTO {

    @NotBlank(message = "Chair ID is required")
    private String chairId;

    @NotBlank(message = "Time slot is required (HH:mm)")
    private String timeSlot;

    private String reason; // e.g. "Lunch Break", "Tea Break"
}
