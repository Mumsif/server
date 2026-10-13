package com.glowslot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object returned for appointment receipts and digital queue passes.
 * Returned by POST /api/v1/public/bookings and GET /api/v1/public/bookings/{id}/pass.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponseDTO {
    private String appointmentId;
    private String ticketNumber;      // e.g. "GS-1042" or "WI-105"
    private String salonName;
    private String salonAddress;
    private String salonImageUrl;
    private String chairId;
    private String chairName;
    private List<ServiceItemDTO> services;
    private String customerName;
    private String customerPhone;
    private String scheduledTime;
    private String estimatedChairTime;
    private int totalDurationMinutes;
    private double totalPriceLkr;
    private String otp;               // 4-digit code (e.g. "4821")
    private int queuePosition;
    private String status;            // "PENDING", "SEATED", "IN_SERVICE", "COMPLETED", "CANCELLED", "NO_SHOW"
    private String createdAt;
}
