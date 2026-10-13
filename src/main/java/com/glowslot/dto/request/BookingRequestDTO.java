package com.glowslot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object received when a customer submits a booking from the Customer Portal.
 * Uses Jakarta Validation constraints to validate incoming JSON payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDTO {

    @NotBlank(message = "Salon ID is required")
    private String salonId;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Customer phone number is required")
    private String customerPhone;

    @NotEmpty(message = "At least one service must be selected")
    private List<String> serviceIds;

    /**
     * Preferred stylist chair: "chair_1", "chair_2", or null / "ANY" for automatic allocation.
     */
    private String preferredChairId;

    @NotBlank(message = "Date is required (YYYY-MM-DD)")
    private String date;

    @NotBlank(message = "Time slot is required (HH:mm)")
    private String timeSlot;
}
