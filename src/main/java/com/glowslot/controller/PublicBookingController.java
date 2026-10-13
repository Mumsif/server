package com.glowslot.controller;

import com.glowslot.dto.request.BookingRequestDTO;
import com.glowslot.dto.response.AppointmentResponseDTO;
import com.glowslot.dto.response.AvailableSlotDTO;
import com.glowslot.dto.response.SalonDetailsDTO;
import com.glowslot.dto.response.ServiceItemDTO;
import com.glowslot.entity.Salon;
import com.glowslot.repository.BarberChairRepository;
import com.glowslot.repository.SalonRepository;
import com.glowslot.repository.ServiceRepository;
import com.glowslot.service.BookingService;
import com.glowslot.service.SlotCalculationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * =========================================================================================
 * CORE STUDY FILE: PublicBookingController
 * =========================================================================================
 * PURPOSE:
 * Public REST Controller serving customer-facing client endpoints (Mobile / Web).
 * Base Path: /api/v1/public
 *
 * ENDPOINTS TO STUDY:
 * 1. GET  /salons/{id}                 -> Salon profile, rating, opening hours, active chairs
 * 2. GET  /salons/{id}/services        -> Catalog of haircuts, beard sculpting, combo services
 * 3. GET  /salons/{id}/slots           -> 45-minute slot matrix computed in real-time
 * 4. POST /bookings                    -> Reserves an appointment atomically and issues OTP + ticket
 * 5. GET  /bookings/{id}/pass          -> Digital pass polling (queue position, estimated time)
 * 6. POST /bookings/{id}/cancel        -> Customer booking cancellation
 * =========================================================================================
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/public")
public class PublicBookingController {

    private final SalonRepository salonRepository;
    private final ServiceRepository serviceRepository;
    private final BarberChairRepository barberChairRepository;
    private final SlotCalculationService slotCalculationService;
    private final BookingService bookingService;

    public PublicBookingController(
            SalonRepository salonRepository,
            ServiceRepository serviceRepository,
            BarberChairRepository barberChairRepository,
            SlotCalculationService slotCalculationService,
            BookingService bookingService
    ) {
        this.salonRepository = salonRepository;
        this.serviceRepository = serviceRepository;
        this.barberChairRepository = barberChairRepository;
        this.slotCalculationService = slotCalculationService;
        this.bookingService = bookingService;
    }

    /**
     * GET /api/v1/public/salons/{id}
     * Fetch salon profile, rating, opening hours, and active styling chairs.
     */
    @GetMapping("/salons/{id}")
    public ResponseEntity<SalonDetailsDTO> getSalonDetails(@PathVariable String id) {
        log.info("Fetching salon details for ID: {}", id);

        Salon salon = salonRepository.findById(id).orElseGet(() -> Salon.builder()
                .id(id)
                .name("Western Cutters")
                .address("Main Street, Akkaraipattu")
                .phone("+94 77 123 4567")
                .imageUrl("https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=800")
                .rating(4.9)
                .reviewCount(128)
                .openingTime("08:15")
                .closingTime("22:30")
                .build());

        List<ServiceItemDTO> services = serviceRepository.findBySalonId(id).stream()
                .map(s -> ServiceItemDTO.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .description(s.getDescription())
                        .durationMinutes(s.getDurationMinutes())
                        .priceLkr(s.getPriceLkr())
                        .category(s.getCategory())
                        .build())
                .collect(Collectors.toList());

        SalonDetailsDTO response = SalonDetailsDTO.builder()
                .id(salon.getId())
                .name(salon.getName())
                .address(salon.getAddress())
                .phone(salon.getPhone())
                .imageUrl(salon.getImageUrl())
                .rating(salon.getRating())
                .reviewCount(salon.getReviewCount())
                .openingTime(salon.getOpeningTime())
                .closingTime(salon.getClosingTime())
                .services(services)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/public/salons/{id}/services
     * Fetch catalog of services (e.g. Normal Haircut, Beard Sculpting).
     */
    @GetMapping("/salons/{id}/services")
    public ResponseEntity<List<ServiceItemDTO>> getSalonServices(@PathVariable String id) {
        log.info("Fetching services catalog for salon ID: {}", id);
        List<ServiceItemDTO> services = serviceRepository.findBySalonId(id).stream()
                .map(s -> ServiceItemDTO.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .description(s.getDescription())
                        .durationMinutes(s.getDurationMinutes())
                        .priceLkr(s.getPriceLkr())
                        .category(s.getCategory())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(services);
    }

    /**
     * GET /api/v1/public/salons/{id}/slots?date=YYYY-MM-DD&stylistId={id}
     * Calculate open 45-min slots for a given date and stylist preference.
     */
    @GetMapping("/salons/{id}/slots")
    public ResponseEntity<List<AvailableSlotDTO>> getAvailableSlots(
            @PathVariable String id,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String stylistId) {
        log.info("Calculating available slots for salon: {}, date: {}, stylist: {}", id, date, stylistId);
        List<AvailableSlotDTO> slots = slotCalculationService.calculateAvailableSlots(id, date, stylistId);
        return ResponseEntity.ok(slots);
    }

    /**
     * POST /api/v1/public/bookings
     * Reserve an appointment atomically and return ticket GS-XXXX and 4-digit OTP.
     */
    @PostMapping("/bookings")
    public ResponseEntity<AppointmentResponseDTO> createBooking(@Valid @RequestBody BookingRequestDTO request) {
        log.info("Received booking request from customer: {}", request.getCustomerName());
        AppointmentResponseDTO response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/public/bookings/{id}/pass
     * Real-time polling for digital queue pass, current queue position, and arrival OTP.
     */
    @GetMapping("/bookings/{id}/pass")
    public ResponseEntity<AppointmentResponseDTO> getBookingPass(@PathVariable String id) {
        AppointmentResponseDTO pass = bookingService.getAppointmentPass(id);
        return ResponseEntity.ok(pass);
    }

    /**
     * POST /api/v1/public/bookings/{id}/cancel
     * Customer booking cancellation.
     */
    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<AppointmentResponseDTO> cancelBooking(@PathVariable String id) {
        AppointmentResponseDTO cancelled = bookingService.cancelBooking(id);
        return ResponseEntity.ok(cancelled);
    }
}
