package com.glowslot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing detailed salon information including available chairs and opening hours.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonDetailsDTO {
    private String id;
    private String name;
    private String address;
    private String phone;
    private String imageUrl;
    private double rating;
    private int reviewCount;
    private String openingTime;
    private String closingTime;
    private List<ServiceItemDTO> services;
    private List<BarberStationChairDTO> chairs;
}
