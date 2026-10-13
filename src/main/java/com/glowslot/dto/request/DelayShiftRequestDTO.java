package com.glowslot.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object sent when a barber triggers a cascading delay shift (+15 minutes).
 * Pushes downstream appointments back dynamically.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelayShiftRequestDTO {

    @NotBlank(message = "Chair ID is required")
    private String chairId;

    private int delayMinutes; // Defaults to 15

    private String reason; // Optional reason, e.g. "Previous haircut required extra styling"
}
