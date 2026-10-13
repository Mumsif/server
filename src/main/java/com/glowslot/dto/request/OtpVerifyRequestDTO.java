package com.glowslot.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object sent by the Barber Workstation when verifying a customer's 4-digit OTP.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyRequestDTO {

    @NotBlank(message = "Chair ID is required")
    private String chairId;

    private String appointmentId;

    @NotBlank(message = "4-digit OTP code is required")
    private String otp;
}
