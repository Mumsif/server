package com.glowslot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing an upcoming customer in the chair queue line.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingQueueItemDTO {
    private String appointmentId;
    private String ticketNumber;
    private String customerName;
    private String serviceNames;
    private String scheduledTime;
    private String status; // "NEXT_IN_LINE" or "RESERVED_WINDOW"
    private String otp;
}
