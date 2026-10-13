package com.glowslot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing a workstation chair with current seated customer and queue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarberStationChairDTO {
    private String chairId;
    private int chairNumber;
    private String barberName;
    private String barberAvatar;
    private String specialization;
    private String status; // "ONLINE", "BUSY", "ON_BREAK"
    private ActiveInChairDTO activeInChair;
    private List<UpcomingQueueItemDTO> upcomingQueue;
}
