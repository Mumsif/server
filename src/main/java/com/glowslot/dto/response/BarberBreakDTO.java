package com.glowslot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a scheduled break taken by a chair/barber.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarberBreakDTO {
    private String id;
    private String chairId;
    private String barberName;
    private String date;
    private String timeSlot;
    private String reason;
    private String createdAt;
}
