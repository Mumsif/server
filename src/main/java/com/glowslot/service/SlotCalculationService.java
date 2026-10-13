package com.glowslot.service;

import com.glowslot.dto.response.AvailableSlotDTO;
import com.glowslot.entity.Appointment;
import com.glowslot.entity.AppointmentStatus;
import com.glowslot.entity.BarberBreak;
import com.glowslot.entity.BarberChair;
import com.glowslot.repository.AppointmentRepository;
import com.glowslot.repository.BarberBreakRepository;
import com.glowslot.repository.BarberChairRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * =========================================================================================
 * CORE STUDY FILE: SlotCalculationService
 * =========================================================================================
 * PURPOSE:
 * Computes real-time 45-minute appointment slots for the customer booking catalog.
 *
 * HOW IT WORKS (THE ALGORITHM):
 * 1. FIXED TIME MATRIX:
 *    The day is divided into twenty 45-minute intervals starting at 08:15 and ending at 22:30.
 * 2. PAST TIME FILTER (FOR TODAY):
 *    If the queried date is today, any slot whose start time is before the current local time
 *    is automatically flagged as "Passed" (isAvailable = false).
 * 3. FAST SET LOOKUPS (O(1)):
 *    Active bookings (excluding CANCELLED) and scheduled barber breaks are fetched from H2 database
 *    and indexed in memory into HashSets using composite keys: "chairId_slotTime" (e.g. "chair_1_10:30").
 * 4. TWO MATCHING MODES:
 *    a) SPECIFIC CHAIR (e.g. customer chooses "chair_1" / Dhanu):
 *       Evaluates that exact chair. If booked -> "Booked". If on break -> "Break". Else -> "Open".
 *    b) "ANY" CHAIR (Auto-Match mode):
 *       Evaluates all active chairs. If at least one chair is free, the slot is marked "Open"
 *       and assigned to that free chair. If all chairs are occupied or on break, it is unavailable.
 * =========================================================================================
 */
@Slf4j
@Service
public class SlotCalculationService {

    private final AppointmentRepository appointmentRepository;
    private final BarberBreakRepository barberBreakRepository;
    private final BarberChairRepository barberChairRepository;

    public SlotCalculationService(
            AppointmentRepository appointmentRepository,
            BarberBreakRepository barberBreakRepository,
            BarberChairRepository barberChairRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.barberBreakRepository = barberBreakRepository;
        this.barberChairRepository = barberChairRepository;
    }

    public static final List<String> STANDARD_SLOTS = List.of(
            "08:15", "09:00", "09:45", "10:30", "11:15", "12:00",
            "12:45", "13:30", "14:15", "15:00", "15:45", "16:30",
            "17:15", "18:00", "18:45", "19:30", "20:15", "21:00",
            "21:45", "22:30"
    );

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Compute slot availability for a salon on a given date and optional chair preference.
     * 
     * @param salonId The salon ID
     * @param dateString Date in YYYY-MM-DD format
     * @param preferredChairId Optional chair ID (e.g. "chair_1", "chair_2", or null/"ANY")
     * @return List of AvailableSlotDTO representing all 45-min intervals
     */
    public List<AvailableSlotDTO> calculateAvailableSlots(String salonId, String dateString, String preferredChairId) {
        LocalDate queryDate;
        try {
            queryDate = LocalDate.parse(dateString);
        } catch (Exception e) {
            queryDate = LocalDate.now();
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        boolean isToday = queryDate.isEqual(today);

        // Fetch all chairs for the salon
        List<BarberChair> chairs = barberChairRepository.findBySalonIdOrderByChairNumberAsc(salonId);
        if (chairs.isEmpty()) {
            chairs = barberChairRepository.findAll();
        }

        // Fetch active bookings and breaks on that date
        List<Appointment> bookings = appointmentRepository.findBySalonIdAndScheduledDateAndStatusNot(
                salonId, dateString, AppointmentStatus.CANCELLED);
        List<BarberBreak> breaks = barberBreakRepository.findByBreakDate(dateString);

        // Build sets of booked and break slots per chair
        // Format of key: chairId + "_" + slotTime
        Set<String> bookedChairSlots = bookings.stream()
                .map(b -> b.getChairId() + "_" + b.getScheduledTime())
                .collect(Collectors.toSet());

        Set<String> breakChairSlots = breaks.stream()
                .map(b -> b.getChairId() + "_" + b.getTimeSlot())
                .collect(Collectors.toSet());

        List<AvailableSlotDTO> result = new ArrayList<>();
        boolean isSpecificChair = preferredChairId != null && !preferredChairId.isBlank() && !"ANY".equalsIgnoreCase(preferredChairId);

        for (String slot : STANDARD_SLOTS) {
            LocalTime slotTime = LocalTime.parse(slot, TIME_FORMATTER);

            // 1. Check if the slot has already passed today
            if (isToday && slotTime.isBefore(now)) {
                result.add(AvailableSlotDTO.builder()
                        .time(slot)
                        .formattedTime(slot)
                        .isAvailable(false)
                        .statusText("Passed")
                        .chairId(preferredChairId)
                        .build());
                continue;
            }

            if (isSpecificChair) {
                // Check single chair availability
                boolean isBooked = bookedChairSlots.contains(preferredChairId + "_" + slot);
                boolean isBreak = breakChairSlots.contains(preferredChairId + "_" + slot);

                if (isBooked) {
                    result.add(AvailableSlotDTO.builder()
                            .time(slot)
                            .formattedTime(slot)
                            .isAvailable(false)
                            .statusText("Booked")
                            .chairId(preferredChairId)
                            .build());
                } else if (isBreak) {
                    result.add(AvailableSlotDTO.builder()
                            .time(slot)
                            .formattedTime(slot)
                            .isAvailable(false)
                            .statusText("Break")
                            .chairId(preferredChairId)
                            .build());
                } else {
                    result.add(AvailableSlotDTO.builder()
                            .time(slot)
                            .formattedTime(slot)
                            .isAvailable(true)
                            .statusText("Open")
                            .chairId(preferredChairId)
                            .build());
                }
            } else {
                // "ANY" chair mode: slot is open if at least one chair is free
                boolean atLeastOneFree = false;
                boolean anyBreak = false;
                String freeChairId = null;

                for (BarberChair chair : chairs) {
                    boolean isBooked = bookedChairSlots.contains(chair.getId() + "_" + slot);
                    boolean isBreak = breakChairSlots.contains(chair.getId() + "_" + slot);

                    if (!isBooked && !isBreak) {
                        atLeastOneFree = true;
                        if (freeChairId == null) {
                            freeChairId = chair.getId();
                        }
                    } else if (isBreak) {
                        anyBreak = true;
                    }
                }

                if (atLeastOneFree) {
                    result.add(AvailableSlotDTO.builder()
                            .time(slot)
                            .formattedTime(slot)
                            .isAvailable(true)
                            .statusText("Open")
                            .chairId(freeChairId)
                            .build());
                } else {
                    result.add(AvailableSlotDTO.builder()
                            .time(slot)
                            .formattedTime(slot)
                            .isAvailable(false)
                            .statusText(anyBreak ? "Break" : "Booked")
                            .chairId(null)
                            .build());
                }
            }
        }

        return result;
    }
}
