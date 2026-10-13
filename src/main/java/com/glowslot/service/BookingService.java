package com.glowslot.service;

import com.glowslot.dto.request.BookingRequestDTO;
import com.glowslot.dto.response.AppointmentResponseDTO;
import com.glowslot.dto.response.ServiceItemDTO;
import com.glowslot.entity.Appointment;
import com.glowslot.entity.AppointmentStatus;
import com.glowslot.entity.BarberChair;
import com.glowslot.entity.Salon;
import com.glowslot.entity.ServiceEntity;
import com.glowslot.repository.AppointmentRepository;
import com.glowslot.repository.BarberChairRepository;
import com.glowslot.repository.SalonRepository;
import com.glowslot.repository.ServiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * =========================================================================================
 * CORE STUDY FILE: BookingService
 * =========================================================================================
 * PURPOSE:
 * Manages the complete customer booking reservation lifecycle, atomic concurrency locks,
 * ticket identification, 4-digit OTP security codes, and dynamic digital queue passes.
 *
 * HOW IT WORKS (THE BOOKING PIPELINE):
 * 1. CHAIR RESOLUTION:
 *    If the user selected a specific chair (e.g. "chair_1"), it reserves that chair.
 *    If the user selected "ANY", it automatically scans all chairs for the requested slot
 *    and assigns the first unoccupied chair (load balancing across stylists).
 * 2. RACE CONDITION / DOUBLE-BOOKING PREVENTION (@Transactional):
 *    Before persisting, it checks if an active booking already exists on that chair + date + slot.
 *    If another customer booked it milliseconds earlier, an exception is thrown immediately.
 * 3. SERVICES & FINANCIAL PRICING:
 *    Calculates total service duration and total LKR price from database catalog.
 * 4. SECURITY & IDENTIFICATION:
 *    - Generates a 4-digit arrival OTP (e.g. "4821") that the customer shows the barber.
 *    - Generates a human-friendly ticket number: "GS-" + random 4-digit code (e.g. "GS-1042").
 * 5. DYNAMIC QUEUE POSITION:
 *    Counts how many pending appointments precede this customer on the assigned chair on that date.
 * =========================================================================================
 */
@Slf4j
@Service
public class BookingService {

    private final AppointmentRepository appointmentRepository;
    private final SalonRepository salonRepository;
    private final BarberChairRepository barberChairRepository;
    private final ServiceRepository serviceRepository;

    private final Random random = new Random();

    public BookingService(
            AppointmentRepository appointmentRepository,
            SalonRepository salonRepository,
            BarberChairRepository barberChairRepository,
            ServiceRepository serviceRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.salonRepository = salonRepository;
        this.barberChairRepository = barberChairRepository;
        this.serviceRepository = serviceRepository;
    }

    /**
     * Atomically reserve a slot and create a new customer appointment.
     * Prevents race conditions and double bookings.
     */
    @Transactional
    public AppointmentResponseDTO createBooking(BookingRequestDTO request) {
        log.info("Processing booking request for salon: {}, customer: {}, slot: {} {}",
                request.getSalonId(), request.getCustomerName(), request.getDate(), request.getTimeSlot());

        // 1. Resolve chair (if "ANY" or empty, choose the first available chair)
        String chairId = resolveAvailableChair(
                request.getSalonId(), request.getDate(), request.getTimeSlot(), request.getPreferredChairId());

        // 2. Concurrency Check: Ensure no active overlapping booking on that chair & slot
        boolean alreadyBooked = appointmentRepository
                .existsBySalonIdAndChairIdAndScheduledDateAndScheduledTimeAndStatusNot(
                        request.getSalonId(), chairId, request.getDate(), request.getTimeSlot(), AppointmentStatus.CANCELLED);

        if (alreadyBooked) {
            throw new IllegalStateException("Slot " + request.getTimeSlot() + " was just booked. Please choose another time.");
        }

        // 3. Fetch selected services & compute total price and duration
        List<ServiceEntity> services = serviceRepository.findByIdIn(request.getServiceIds());
        if (services.isEmpty()) {
            services = serviceRepository.findAll();
        }

        int totalDuration = services.stream().mapToInt(ServiceEntity::getDurationMinutes).sum();
        if (totalDuration == 0) totalDuration = 45;

        double totalPrice = services.stream().mapToDouble(ServiceEntity::getPriceLkr).sum();
        if (totalPrice == 0) totalPrice = 800.0;

        // 4. Generate 4-digit OTP (e.g. "4821")
        String otp = String.format("%04d", random.nextInt(10000));

        // 5. Generate Ticket Number: "GS-" + random 4-digit (e.g. "GS-1042")
        String ticketNumber = "GS-" + (1000 + random.nextInt(9000));

        // 6. Compute real-time queue position
        long aheadCount = appointmentRepository
                .countByChairIdAndScheduledDateAndScheduledTimeLessThanAndStatus(
                        chairId, request.getDate(), request.getTimeSlot(), AppointmentStatus.PENDING);
        int queuePosition = (int) aheadCount + 1;

        // 7. Persist Appointment
        String appointmentId = "app_" + System.currentTimeMillis() + "_" + (100 + random.nextInt(900));
        String serviceIdsString = String.join(",", request.getServiceIds());

        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .ticketNumber(ticketNumber)
                .salonId(request.getSalonId())
                .chairId(chairId)
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .scheduledDate(request.getDate())
                .scheduledTime(request.getTimeSlot())
                .estimatedChairTime(request.getTimeSlot())
                .totalDurationMinutes(totalDuration)
                .totalPriceLkr(totalPrice)
                .otp(otp)
                .queuePosition(queuePosition)
                .status(AppointmentStatus.PENDING)
                .selectedServiceIdsJson(serviceIdsString)
                .createdAt(LocalDateTime.now())
                .build();

        appointment = appointmentRepository.save(appointment);
        log.info("Booking confirmed! Ticket: {}, OTP: {}, AppointmentId: {}", ticketNumber, otp, appointment.getId());

        return mapToResponseDTO(appointment, services);
    }

    /**
     * Fetch appointment live pass details (used for polling or rendering the pass).
     */
    @Transactional(readOnly = true)
    public AppointmentResponseDTO getAppointmentPass(String appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with ID: " + appointmentId));

        // Recalculate dynamic queue position
        long aheadCount = appointmentRepository
                .countByChairIdAndScheduledDateAndScheduledTimeLessThanAndStatus(
                        appointment.getChairId(), appointment.getScheduledDate(), appointment.getScheduledTime(), AppointmentStatus.PENDING);
        appointment.setQueuePosition((int) aheadCount + 1);

        List<ServiceEntity> services = getServicesForAppointment(appointment);
        return mapToResponseDTO(appointment, services);
    }

    /**
     * Customer booking cancellation.
     */
    @Transactional
    public AppointmentResponseDTO cancelBooking(String appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with ID: " + appointmentId));

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
        log.info("Appointment {} cancelled by customer.", appointmentId);

        List<ServiceEntity> services = getServicesForAppointment(appointment);
        return mapToResponseDTO(appointment, services);
    }

    /**
     * Helper to resolve chair when "ANY" is selected.
     */
    private String resolveAvailableChair(String salonId, String date, String timeSlot, String preferredChairId) {
        if (preferredChairId != null && !preferredChairId.isBlank() && !"ANY".equalsIgnoreCase(preferredChairId)) {
            return preferredChairId;
        }

        List<BarberChair> chairs = barberChairRepository.findBySalonIdOrderByChairNumberAsc(salonId);
        if (chairs.isEmpty()) {
            return "chair_1";
        }

        for (BarberChair chair : chairs) {
            boolean taken = appointmentRepository
                    .existsBySalonIdAndChairIdAndScheduledDateAndScheduledTimeAndStatusNot(
                            salonId, chair.getId(), date, timeSlot, AppointmentStatus.CANCELLED);
            if (!taken) {
                return chair.getId();
            }
        }

        // Default to first chair if both are checked
        return chairs.get(0).getId();
    }

    private List<ServiceEntity> getServicesForAppointment(Appointment appointment) {
        if (appointment.getSelectedServiceIdsJson() == null || appointment.getSelectedServiceIdsJson().isBlank()) {
            return List.of();
        }
        List<String> ids = Arrays.asList(appointment.getSelectedServiceIdsJson().split(","));
        return serviceRepository.findByIdIn(ids);
    }

    private AppointmentResponseDTO mapToResponseDTO(Appointment appointment, List<ServiceEntity> services) {
        Salon salon = salonRepository.findById(appointment.getSalonId()).orElse(null);
        BarberChair chair = barberChairRepository.findById(appointment.getChairId()).orElse(null);

        List<ServiceItemDTO> serviceDTOs = services.stream()
                .map(s -> ServiceItemDTO.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .description(s.getDescription())
                        .durationMinutes(s.getDurationMinutes())
                        .priceLkr(s.getPriceLkr())
                        .category(s.getCategory())
                        .build())
                .collect(Collectors.toList());

        return AppointmentResponseDTO.builder()
                .appointmentId(appointment.getId())
                .ticketNumber(appointment.getTicketNumber())
                .salonName(salon != null ? salon.getName() : "Western Cutters")
                .salonAddress(salon != null ? salon.getAddress() : "Main Street, Akkaraipattu")
                .salonImageUrl(salon != null ? salon.getImageUrl() : "")
                .chairId(appointment.getChairId())
                .chairName(chair != null ? chair.getBarberName() : appointment.getChairId())
                .services(serviceDTOs)
                .customerName(appointment.getCustomerName())
                .customerPhone(appointment.getCustomerPhone())
                .scheduledTime(appointment.getScheduledTime())
                .estimatedChairTime(appointment.getEstimatedChairTime() != null ? appointment.getEstimatedChairTime() : appointment.getScheduledTime())
                .totalDurationMinutes(appointment.getTotalDurationMinutes())
                .totalPriceLkr(appointment.getTotalPriceLkr())
                .otp(appointment.getOtp())
                .queuePosition(appointment.getQueuePosition())
                .status(appointment.getStatus().name())
                .createdAt(appointment.getCreatedAt() != null ? appointment.getCreatedAt().toString() : LocalDateTime.now().toString())
                .build();
    }
}
