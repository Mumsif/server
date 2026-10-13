package com.glowslot.controller;

import com.glowslot.dto.request.BreakRequestDTO;
import com.glowslot.dto.request.DelayShiftRequestDTO;
import com.glowslot.dto.request.OtpVerifyRequestDTO;
import com.glowslot.dto.request.WalkInRequestDTO;
import com.glowslot.dto.response.AppointmentResponseDTO;
import com.glowslot.dto.response.BarberBreakDTO;
import com.glowslot.dto.response.WorkstationStateDTO;
import com.glowslot.service.QueueManagementService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * =========================================================================================
 * CORE STUDY FILE: BarberController
 * =========================================================================================
 * PURPOSE:
 * REST Controller serving the Barber Workstation operations portal (Tablet/Desktop).
 * Base Path: /api/v1/barber
 *
 * ENDPOINTS TO STUDY:
 * 1. GET  /workstation/{salonId}       -> Live workstation state (chair timers, queues, telemetry, wallet)
 * 2. POST /verify-otp                  -> Barber inputs 4-digit customer OTP -> Seats customer -> Starts timer
 * 3. POST /complete-service/{id}       -> Completes haircut -> Auto-deducts LKR 50 platform fee from wallet
 * 4. POST /walk-in                     -> Quick walk-in registration (ticket WI-XXX)
 * 5. POST /delay-shift                 -> Cascades a +15 min delay to waiting customers
 * 6. POST /break                       -> Schedules barber meal/tea break
 * 7. POST /chairs/{id}/toggle-status   -> Toggles chair state between ONLINE and ON_BREAK
 * =========================================================================================
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/barber")
public class BarberController {

    private final QueueManagementService queueManagementService;

    public BarberController(QueueManagementService queueManagementService) {
        this.queueManagementService = queueManagementService;
    }

    /**
     * GET /api/v1/barber/workstation/{salonId}
     * Returns full live state of all chairs, countdown timers, upcoming queues, telemetry, and wallet.
     */
    @GetMapping("/workstation/{salonId}")
    public ResponseEntity<WorkstationStateDTO> getWorkstationState(@PathVariable String salonId) {
        log.info("Loading workstation dashboard state for salon: {}", salonId);
        WorkstationStateDTO state = queueManagementService.getWorkstationState(salonId);
        return ResponseEntity.ok(state);
    }

    /**
     * POST /api/v1/barber/verify-otp
     * Validates customer's 4-digit code and changes status to SEATED (starts chair timer).
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody OtpVerifyRequestDTO dto) {
        log.info("Barber verifying OTP: {} for chair: {}", dto.getOtp(), dto.getChairId());
        Map<String, Object> result = queueManagementService.verifyOtp(dto);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/v1/barber/complete-service/{id}
     * Marks appointment COMPLETED and deducts LKR 50 commission from salon wallet.
     */
    @PostMapping("/complete-service/{id}")
    public ResponseEntity<Map<String, Object>> completeService(@PathVariable String id) {
        log.info("Barber completed service for appointment: {}", id);
        Map<String, Object> result = queueManagementService.completeService(id);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/v1/barber/walk-in
     * Registers and seats/queues an immediate walk-in guest (WI-XXX).
     */
    @PostMapping("/walk-in")
    public ResponseEntity<AppointmentResponseDTO> registerWalkIn(@Valid @RequestBody WalkInRequestDTO dto) {
        log.info("Registering walk-in guest: {} on chair: {}", dto.getCustomerName(), dto.getChairId());
        AppointmentResponseDTO response = queueManagementService.registerWalkIn(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/v1/barber/delay-shift
     * Applies a cascading +15m delay to all downstream pending bookings on a chair.
     */
    @PostMapping("/delay-shift")
    public ResponseEntity<Map<String, Object>> applyDelayShift(@Valid @RequestBody DelayShiftRequestDTO dto) {
        log.info("Applying delay shift of {} mins on chair: {}", dto.getDelayMinutes(), dto.getChairId());
        Map<String, Object> result = queueManagementService.applyDelayShift(dto);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/v1/barber/break
     * Schedules a break (Tea/Lunch) which blocks customer slot availability.
     */
    @PostMapping("/break")
    public ResponseEntity<BarberBreakDTO> scheduleBreak(@Valid @RequestBody BreakRequestDTO dto) {
        log.info("Scheduling break for chair: {} at slot: {}", dto.getChairId(), dto.getTimeSlot());
        BarberBreakDTO result = queueManagementService.scheduleBreak(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * POST /api/v1/barber/break/{chairId}/end
     * Ends active break and sets chair status back to ONLINE.
     */
    @PostMapping("/break/{chairId}/end")
    public ResponseEntity<Map<String, Object>> endBreak(@PathVariable String chairId) {
        log.info("Ending break for chair: {}", chairId);
        Map<String, Object> result = queueManagementService.endBreak(chairId);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/v1/barber/chair/{chairId}/toggle-status
     * Toggles chair status ONLINE <-> ON_BREAK.
     */
    @PostMapping("/chair/{chairId}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleChairStatus(@PathVariable String chairId) {
        log.info("Toggling status for chair: {}", chairId);
        Map<String, Object> result = queueManagementService.toggleChairStatus(chairId);
        return ResponseEntity.ok(result);
    }
}
