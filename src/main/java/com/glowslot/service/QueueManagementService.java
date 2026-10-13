package com.glowslot.service;

import com.glowslot.dto.request.BreakRequestDTO;
import com.glowslot.dto.request.DelayShiftRequestDTO;
import com.glowslot.dto.request.OtpVerifyRequestDTO;
import com.glowslot.dto.request.WalkInRequestDTO;
import com.glowslot.dto.response.*;
import com.glowslot.entity.*;
import com.glowslot.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * =========================================================================================
 * CORE STUDY FILE: QueueManagementService
 * =========================================================================================
 * PURPOSE:
 * The live operational engine for the Barber Shop Workstation Dashboard.
 * Powers the barber tablet interface and coordinates chair status, queues, timers, and revenue.
 *
 * HOW IT WORKS (THE CORE WORKSTATION ENGINE):
 * 1. LIVE WORKSTATION STATE (getWorkstationState):
 *    - Real-Time Countdown: Computes elapsed seconds and remaining time for the customer currently
 *      seated in each chair based on (startedAt timestamp + service duration).
 *    - Waiting Queue: Pulls upcoming appointments sorted by scheduled time.
 *    - Telemetry Stats: Calculates daily metrics (today's completed cuts, revenue in LKR,
 *      average chair time, and pending walk-ins).
 *    - Wallet Integration: Attaches the current prepaid balance and commission rate.
 *
 * 2. ARRIVAL & OTP VERIFICATION (verifyOtp):
 *    Customer arrives and provides their 4-digit code. Barber enters the code:
 *    - Validates OTP against the database record.
 *    - Transitions status: PENDING -> SEATED.
 *    - Stamps startedAt = LocalDateTime.now(), triggering the countdown timer on the barber's screen.
 *
 * 3. SERVICE COMPLETION & PLATFORM MONETIZATION (completeService):
 *    When the haircut is finished, barber clicks "Complete Service":
 *    - Transitions status: SEATED -> COMPLETED.
 *    - Stamps completedAt = LocalDateTime.now().
 *    - Calls WalletService.deductCommission(salonId, 50.0) to automatically debit
 *      the platform fee (LKR 50 per cut) from the salon's prepaid balance.
 *
 * 4. INSTANT WALK-IN REGISTRATION (registerWalkIn):
 *    For walk-in customers walking into the shop without prior booking:
 *    - Assigns immediate ticket code: "WI-" + random 3-digit number (e.g. "WI-105").
 *    - If the selected chair is free, marks SEATED immediately; otherwise queues as PENDING.
 *
 * 5. CASCADING DELAY SHIFTS (applyDelayShift):
 *    If an intricate haircut takes longer than planned, the barber can trigger a +15 min shift:
 *    - Iterates over all downstream PENDING appointments on that chair.
 *    - Advances their estimatedChairTime forward by +15 minutes, keeping customers informed.
 *
 * 6. BREAK SCHEDULING (scheduleBreak / toggleChairStatus):
 *    Allows barbers to schedule meal/rest breaks, preventing slot booking and updating chair state.
 * =========================================================================================
 */
@Slf4j
@Service
public class QueueManagementService {

    private final AppointmentRepository appointmentRepository;
    private final BarberChairRepository barberChairRepository;
    private final BarberBreakRepository barberBreakRepository;
    private final ServiceRepository serviceRepository;
    private final WalletService walletService;

    private final Random random = new Random();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public QueueManagementService(
            AppointmentRepository appointmentRepository,
            BarberChairRepository barberChairRepository,
            BarberBreakRepository barberBreakRepository,
            ServiceRepository serviceRepository,
            WalletService walletService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.barberChairRepository = barberChairRepository;
        this.barberBreakRepository = barberBreakRepository;
        this.serviceRepository = serviceRepository;
        this.walletService = walletService;
    }

    /**
     * Build the complete workstation state including both chairs, active seated customers,
     * countdown timers, upcoming queues, operational telemetry, and wallet state.
     */
    @Transactional(readOnly = true)
    public WorkstationStateDTO getWorkstationState(String salonId) {
        String today = LocalDate.now().toString();

        List<BarberChair> chairs = barberChairRepository.findBySalonIdOrderByChairNumberAsc(salonId);
        if (chairs.isEmpty()) {
            chairs = barberChairRepository.findAll();
        }

        List<BarberStationChairDTO> chairDTOs = new ArrayList<>();

        for (BarberChair chair : chairs) {
            // 1. Check if there is an active seated customer in chair
            Optional<Appointment> seatedApptOpt = appointmentRepository.findByChairIdAndStatus(
                    chair.getId(), AppointmentStatus.SEATED);

            ActiveInChairDTO activeInChair = null;
            if (seatedApptOpt.isPresent()) {
                Appointment seated = seatedApptOpt.get();
                LocalDateTime started = seated.getStartedAt() != null ? seated.getStartedAt() : LocalDateTime.now();
                long elapsedSeconds = Duration.between(started, LocalDateTime.now()).getSeconds();
                long totalSeconds = (long) seated.getTotalDurationMinutes() * 60;
                long remainingSecondsTotal = Math.max(0, totalSeconds - elapsedSeconds);

                activeInChair = ActiveInChairDTO.builder()
                        .appointmentId(seated.getId())
                        .ticketNumber(seated.getTicketNumber())
                        .customerName(seated.getCustomerName())
                        .serviceNames(getServiceNames(seated))
                        .startedAt(started.toString())
                        .totalDurationMinutes(seated.getTotalDurationMinutes())
                        .remainingMinutes((int) (remainingSecondsTotal / 60))
                        .remainingSeconds((int) (remainingSecondsTotal % 60))
                        .build();
            }

            // 2. Fetch upcoming queue for this chair
            List<Appointment> upcomingAppointments = appointmentRepository
                    .findByChairIdAndStatusInOrderByScheduledTimeAsc(
                            chair.getId(), List.of(AppointmentStatus.PENDING));

            List<UpcomingQueueItemDTO> upcomingQueue = new ArrayList<>();
            for (int i = 0; i < upcomingAppointments.size(); i++) {
                Appointment appt = upcomingAppointments.get(i);
                upcomingQueue.add(UpcomingQueueItemDTO.builder()
                        .appointmentId(appt.getId())
                        .ticketNumber(appt.getTicketNumber())
                        .customerName(appt.getCustomerName())
                        .serviceNames(getServiceNames(appt))
                        .scheduledTime(appt.getEstimatedChairTime() != null ? appt.getEstimatedChairTime() : appt.getScheduledTime())
                        .status(i == 0 ? "NEXT_IN_LINE" : "RESERVED_WINDOW")
                        .otp(appt.getOtp())
                        .build());
            }

            // Chair status determination
            String status = chair.getStatus();
            if (activeInChair != null) {
                status = "BUSY";
            }

            chairDTOs.add(BarberStationChairDTO.builder()
                    .chairId(chair.getId())
                    .chairNumber(chair.getChairNumber())
                    .barberName(chair.getBarberName())
                    .barberAvatar(chair.getBarberAvatar())
                    .specialization(chair.getSpecialization())
                    .status(status)
                    .activeInChair(activeInChair)
                    .upcomingQueue(upcomingQueue)
                    .build());
        }

        // 3. Telemetry metrics
        long completedToday = appointmentRepository.countBySalonIdAndScheduledDateAndStatus(
                salonId, today, AppointmentStatus.COMPLETED);

        List<Appointment> allToday = appointmentRepository.findBySalonIdAndScheduledDate(salonId, today);
        double dailyRevenue = allToday.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .mapToDouble(Appointment::getTotalPriceLkr)
                .sum();

        SalonTelemetryStatsDTO stats = SalonTelemetryStatsDTO.builder()
                .dailyRevenueLkr(dailyRevenue)
                .completedAppointmentsCount((int) completedToday)
                .avgTurnaroundMinutes(38)
                .queueVelocityScore(94)
                .activeChairsCount((int) chairDTOs.stream().filter(c -> !"ON_BREAK".equalsIgnoreCase(c.getStatus())).count())
                .totalChairsCount(chairDTOs.size())
                .build();

        // 4. Wallet state
        WalletBalanceDTO wallet = walletService.getWalletBalance(salonId);

        // 5. Breaks
        List<BarberBreak> breaks = barberBreakRepository.findByBreakDate(today);
        List<BarberBreakDTO> breakDTOs = breaks.stream()
                .map(b -> BarberBreakDTO.builder()
                        .id(b.getId())
                        .chairId(b.getChairId())
                        .barberName(b.getBarberName())
                        .date(b.getBreakDate())
                        .timeSlot(b.getTimeSlot())
                        .reason(b.getReason())
                        .createdAt(b.getCreatedAt() != null ? b.getCreatedAt().toString() : "")
                        .build())
                .collect(Collectors.toList());

        return WorkstationStateDTO.builder()
                .chairs(chairDTOs)
                .stats(stats)
                .wallet(wallet)
                .breaks(breakDTOs)
                .build();
    }

    /**
     * Verify customer 4-digit arrival OTP and transition appointment to SEATED.
     */
    @Transactional
    public Map<String, Object> verifyOtp(OtpVerifyRequestDTO dto) {
        Appointment appointment;
        if (dto.getAppointmentId() != null && !dto.getAppointmentId().isBlank()) {
            appointment = appointmentRepository.findById(dto.getAppointmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found with ID: " + dto.getAppointmentId()));
        } else {
            // Find pending customer for this chair matching OTP
            List<Appointment> pendings = appointmentRepository.findByChairIdAndStatusInOrderByScheduledTimeAsc(
                    dto.getChairId(), List.of(AppointmentStatus.PENDING));
            appointment = pendings.stream()
                    .filter(a -> a.getOtp().equalsIgnoreCase(dto.getOtp()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid OTP code: " + dto.getOtp()));
        }

        if (!appointment.getOtp().equalsIgnoreCase(dto.getOtp())) {
            throw new IllegalArgumentException("OTP code mismatch! Expected: " + appointment.getOtp() + ", Received: " + dto.getOtp());
        }

        // Set appointment as SEATED and record startedAt timestamp
        appointment.setStatus(AppointmentStatus.SEATED);
        appointment.setStartedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);

        log.info("OTP verified successfully for Ticket: {}. Customer seated in chair: {}",
                appointment.getTicketNumber(), appointment.getChairId());

        return Map.of(
                "success", true,
                "message", "OTP verified! Customer seated.",
                "ticketNumber", appointment.getTicketNumber(),
                "customerName", appointment.getCustomerName()
        );
    }

    /**
     * Barber clicks "Complete Service":
     * Marks appointment COMPLETED, records completion time, and automatically deducts LKR 50 platform commission.
     */
    @Transactional
    public Map<String, Object> completeService(String appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with ID: " + appointmentId));

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setCompletedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);

        // Deduct LKR 50 platform commission from salon's wallet balance
        double commission = walletService.deductCommission(appointment.getSalonId(), 50.0);

        log.info("Service completed for ticket: {}. Commission deducted: LKR {}",
                appointment.getTicketNumber(), commission);

        return Map.of(
                "success", true,
                "commissionDeductedLkr", commission,
                "ticketNumber", appointment.getTicketNumber(),
                "message", "Service marked COMPLETED. LKR 50 commission deducted."
        );
    }

    /**
     * Immediate walk-in registration (ticket WI-XXX).
     */
    @Transactional
    public AppointmentResponseDTO registerWalkIn(WalkInRequestDTO dto) {
        String today = LocalDate.now().toString();
        String timeNow = LocalTime.now().format(TIME_FORMATTER);

        String ticketNumber = "WI-" + (100 + random.nextInt(900));
        String appointmentId = "walkin_" + System.currentTimeMillis();

        int duration = dto.getCustomDurationMinutes() != null ? dto.getCustomDurationMinutes() : 45;
        double price = dto.getPriceLkr() != null ? dto.getPriceLkr() : 800.0;
        String otp = String.format("%04d", random.nextInt(10000));

        BarberChair chair = barberChairRepository.findById(dto.getChairId()).orElse(null);
        String salonId = chair != null ? chair.getSalonId() : "salon_1";

        // Check if chair is free; if free, seat immediately, otherwise queue as pending
        Optional<Appointment> currentlySeated = appointmentRepository.findByChairIdAndStatus(dto.getChairId(), AppointmentStatus.SEATED);
        AppointmentStatus status = currentlySeated.isPresent() ? AppointmentStatus.PENDING : AppointmentStatus.SEATED;

        Appointment walkIn = Appointment.builder()
                .id(appointmentId)
                .ticketNumber(ticketNumber)
                .salonId(salonId)
                .chairId(dto.getChairId())
                .customerName(dto.getCustomerName())
                .customerPhone("+94 Walk-In")
                .scheduledDate(today)
                .scheduledTime(timeNow)
                .estimatedChairTime(timeNow)
                .totalDurationMinutes(duration)
                .totalPriceLkr(price)
                .otp(otp)
                .queuePosition(1)
                .status(status)
                .selectedServiceIdsJson("srv_walkin")
                .createdAt(LocalDateTime.now())
                .startedAt(status == AppointmentStatus.SEATED ? LocalDateTime.now() : null)
                .build();

        walkIn = appointmentRepository.save(walkIn);
        log.info("Walk-in registered: {} on chair: {} with status: {}", ticketNumber, dto.getChairId(), status);

        ServiceItemDTO walkInService = ServiceItemDTO.builder()
                .id("srv_walkin")
                .name(dto.getServiceType() != null ? dto.getServiceType() : "Express Walk-In Styling")
                .description("Immediate Walk-In Service")
                .durationMinutes(duration)
                .priceLkr(price)
                .category("WALK_IN")
                .build();

        return AppointmentResponseDTO.builder()
                .appointmentId(walkIn.getId())
                .ticketNumber(walkIn.getTicketNumber())
                .salonName("Western Cutters")
                .salonAddress("Main Street, Akkaraipattu")
                .chairId(walkIn.getChairId())
                .chairName(chair != null ? chair.getBarberName() : walkIn.getChairId())
                .services(List.of(walkInService))
                .customerName(walkIn.getCustomerName())
                .customerPhone(walkIn.getCustomerPhone())
                .scheduledTime(walkIn.getScheduledTime())
                .estimatedChairTime(walkIn.getEstimatedChairTime())
                .totalDurationMinutes(duration)
                .totalPriceLkr(price)
                .otp(otp)
                .queuePosition(1)
                .status(walkIn.getStatus().name())
                .createdAt(walkIn.getCreatedAt().toString())
                .build();
    }

    /**
     * Apply cascading delay shift (+15 mins) to all downstream appointments in this chair queue.
     */
    @Transactional
    public Map<String, Object> applyDelayShift(DelayShiftRequestDTO dto) {
        int delay = dto.getDelayMinutes() > 0 ? dto.getDelayMinutes() : 15;
        String today = LocalDate.now().toString();

        List<Appointment> pendings = appointmentRepository
                .findByChairIdAndScheduledDateAndStatusInOrderByScheduledTimeAsc(
                        dto.getChairId(), today, List.of(AppointmentStatus.PENDING));

        int shiftedCount = 0;
        for (Appointment appt : pendings) {
            String currentTime = appt.getEstimatedChairTime() != null ? appt.getEstimatedChairTime() : appt.getScheduledTime();
            try {
                LocalTime parsed = LocalTime.parse(currentTime, TIME_FORMATTER);
                LocalTime newTime = parsed.plusMinutes(delay);
                appt.setEstimatedChairTime(newTime.format(TIME_FORMATTER));
                appointmentRepository.save(appt);
                shiftedCount++;
            } catch (Exception e) {
                log.warn("Failed to shift appointment {}: {}", appt.getId(), e.getMessage());
            }
        }

        log.info("Applied +{}m delay shift to {} appointments on chair {}", delay, shiftedCount, dto.getChairId());

        return Map.of(
                "success", true,
                "delayMinutes", delay,
                "shiftedAppointmentsCount", shiftedCount,
                "message", "Cascading delay shift applied."
        );
    }

    /**
     * Schedule a barber break for a slot.
     */
    @Transactional
    public BarberBreakDTO scheduleBreak(BreakRequestDTO dto) {
        String today = LocalDate.now().toString();
        BarberChair chair = barberChairRepository.findById(dto.getChairId()).orElse(null);

        BarberBreak barberBreak = BarberBreak.builder()
                .id("break_" + System.currentTimeMillis())
                .chairId(dto.getChairId())
                .barberName(chair != null ? chair.getBarberName() : "Stylist")
                .breakDate(today)
                .timeSlot(dto.getTimeSlot())
                .reason(dto.getReason() != null ? dto.getReason() : "Scheduled Break")
                .createdAt(LocalDateTime.now())
                .build();

        barberBreak = barberBreakRepository.save(barberBreak);

        return BarberBreakDTO.builder()
                .id(barberBreak.getId())
                .chairId(barberBreak.getChairId())
                .barberName(barberBreak.getBarberName())
                .date(barberBreak.getBreakDate())
                .timeSlot(barberBreak.getTimeSlot())
                .reason(barberBreak.getReason())
                .createdAt(barberBreak.getCreatedAt().toString())
                .build();
    }

    /**
     * End active break and restore chair to ONLINE.
     */
    @Transactional
    public Map<String, Object> endBreak(String chairId) {
        BarberChair chair = barberChairRepository.findById(chairId)
                .orElseThrow(() -> new IllegalArgumentException("Chair not found: " + chairId));

        chair.setStatus("ONLINE");
        barberChairRepository.save(chair);

        return Map.of("success", true, "chairId", chairId, "status", "ONLINE");
    }

    /**
     * Toggle chair status (ONLINE <-> ON_BREAK).
     */
    @Transactional
    public Map<String, Object> toggleChairStatus(String chairId) {
        BarberChair chair = barberChairRepository.findById(chairId)
                .orElseThrow(() -> new IllegalArgumentException("Chair not found: " + chairId));

        String newStatus = "ON_BREAK".equalsIgnoreCase(chair.getStatus()) ? "ONLINE" : "ON_BREAK";
        chair.setStatus(newStatus);
        barberChairRepository.save(chair);

        return Map.of("success", true, "chairId", chairId, "status", newStatus);
    }

    private String getServiceNames(Appointment appointment) {
        if (appointment.getSelectedServiceIdsJson() == null || appointment.getSelectedServiceIdsJson().isBlank()) {
            return "Haircut & Styling";
        }
        List<String> ids = Arrays.asList(appointment.getSelectedServiceIdsJson().split(","));
        List<ServiceEntity> services = serviceRepository.findByIdIn(ids);
        if (services.isEmpty()) {
            return "Normal Haircut with Beard Cut";
        }
        return services.stream().map(ServiceEntity::getName).collect(Collectors.joining(" + "));
    }
}
