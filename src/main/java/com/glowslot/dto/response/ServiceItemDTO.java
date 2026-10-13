package com.glowslot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing an individual service item returned in catalogs or booking receipts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceItemDTO {
    private String id;
    private String name;
    private String description;
    private int durationMinutes;
    private double priceLkr;
    private String category;
}
