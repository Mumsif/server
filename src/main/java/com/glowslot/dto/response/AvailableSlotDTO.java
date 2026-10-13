package com.glowslot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing an individual 45-minute booking slot.
 * Returned by GET /api/v1/public/salons/{id}/slots
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableSlotDTO {
    private String time;             // "10:30"
    private String formattedTime;    // "10:30"
    private boolean isAvailable;     // true/false
    private String statusText;       // "Open", "Booked", "Break", "Passed"
    private String chairId;          // null if ANY, or "chair_1"/"chair_2"
}
