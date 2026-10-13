package com.glowslot.config;

import com.glowslot.entity.*;
import com.glowslot.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds initial demo data on application startup so the frontend can immediately
 * connect and interact with pre-configured salons, chairs, services, and wallet balances.
 */
@Slf4j
@Configuration
public class DataInitializer implements CommandLineRunner {

    private final SalonRepository salonRepository;
    private final BarberChairRepository barberChairRepository;
    private final ServiceRepository serviceRepository;
    private final SalonWalletRepository salonWalletRepository;
    private final AppointmentRepository appointmentRepository;

    public DataInitializer(
            SalonRepository salonRepository,
            BarberChairRepository barberChairRepository,
            ServiceRepository serviceRepository,
            SalonWalletRepository salonWalletRepository,
            AppointmentRepository appointmentRepository
    ) {
        this.salonRepository = salonRepository;
        this.barberChairRepository = barberChairRepository;
        this.serviceRepository = serviceRepository;
        this.salonWalletRepository = salonWalletRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public void run(String... args) {
        log.info("Checking and seeding Trimly GlowSlot initial database records...");

        // 1. Seed Salon
        if (salonRepository.count() == 0) {
            Salon salon = Salon.builder()
                    .id("salon_1")
                    .name("Western Cutters")
                    .address("Main Street, Akkaraipattu")
                    .phone("+94 67 227 8900")
                    .imageUrl("https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=800")
                    .rating(4.9)
                    .reviewCount(128)
                    .openingTime("08:15")
                    .closingTime("22:30")
                    .build();
            salonRepository.save(salon);
            log.info("Seeded salon: {}", salon.getName());
        }

        // 2. Seed Chairs
        if (barberChairRepository.count() == 0) {
            BarberChair chair1 = BarberChair.builder()
                    .id("chair_1")
                    .salonId("salon_1")
                    .chairNumber(1)
                    .barberName("Dhanu")
                    .barberAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=Dhanu")
                    .specialization("Fade Master & Beard Sculpting")
                    .status("ONLINE")
                    .build();

            BarberChair chair2 = BarberChair.builder()
                    .id("chair_2")
                    .salonId("salon_1")
                    .chairNumber(2)
                    .barberName("Thambi")
                    .barberAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=Thambi")
                    .specialization("Classic Scissor Cut & Styling")
                    .status("ONLINE")
                    .build();

            barberChairRepository.saveAll(List.of(chair1, chair2));
            log.info("Seeded chairs: Chair 1 (Dhanu), Chair 2 (Thambi)");
        }

        // 3. Seed Services Catalog
        if (serviceRepository.count() == 0) {
            ServiceEntity s1 = ServiceEntity.builder()
                    .id("srv_1")
                    .salonId("salon_1")
                    .name("Normal Haircut Only")
                    .description("Precision scissor or clipper haircut with clean neck shave")
                    .durationMinutes(45)
                    .priceLkr(400.0)
                    .category("HAIRCUT")
                    .build();

            ServiceEntity s2 = ServiceEntity.builder()
                    .id("srv_2")
                    .salonId("salon_1")
                    .name("Normal Haircut with Beard Cut")
                    .description("Signature full grooming combo: Haircut, hot lather beard shape & razor line")
                    .durationMinutes(45)
                    .priceLkr(800.0)
                    .category("COMBO")
                    .build();

            ServiceEntity s3 = ServiceEntity.builder()
                    .id("srv_3")
                    .salonId("salon_1")
                    .name("Beard Trim & Razor Sculpt")
                    .description("Detailed beard detailing, fade, trimming and oil treatment")
                    .durationMinutes(30)
                    .priceLkr(300.0)
                    .category("BEARD")
                    .build();

            ServiceEntity s4 = ServiceEntity.builder()
                    .id("srv_4")
                    .salonId("salon_1")
                    .name("Head Massage & Hair Wash")
                    .description("Relaxing herbal oil head massage and refreshing wash")
                    .durationMinutes(30)
                    .priceLkr(500.0)
                    .category("TREATMENT")
                    .build();

            serviceRepository.saveAll(List.of(s1, s2, s3, s4));
            log.info("Seeded 4 services in the catalog.");
        }

        // 4. Seed Salon Wallet Balance
        if (salonWalletRepository.count() == 0) {
            SalonWallet wallet = SalonWallet.builder()
                    .salonId("salon_1")
                    .balanceLkr(2450.0)
                    .currency("LKR")
                    .commissionPerCutLkr(50.0)
                    .lastUpdated(LocalDateTime.now())
                    .build();
            salonWalletRepository.save(wallet);
            log.info("Seeded salon wallet: LKR {} balance.", wallet.getBalanceLkr());
        }

        // 5. Seed sample appointments (1 seated customer and 1 upcoming in queue for Chair 1)
        if (appointmentRepository.count() == 0) {
            String today = LocalDate.now().toString();

            // Seated customer currently in Chair 1
            Appointment seated = Appointment.builder()
                    .id("app_demo_seated_1")
                    .ticketNumber("GS-1042")
                    .salonId("salon_1")
                    .chairId("chair_1")
                    .customerName("Kavindu Silva")
                    .customerPhone("+94 77 987 6543")
                    .scheduledDate(today)
                    .scheduledTime("11:15")
                    .estimatedChairTime("11:15")
                    .totalDurationMinutes(45)
                    .totalPriceLkr(800.0)
                    .otp("4821")
                    .queuePosition(0)
                    .status(AppointmentStatus.SEATED)
                    .selectedServiceIdsJson("srv_2")
                    .createdAt(LocalDateTime.now().minusMinutes(20))
                    .startedAt(LocalDateTime.now().minusMinutes(15))
                    .build();

            // Next in line for Chair 1
            Appointment upcoming1 = Appointment.builder()
                    .id("app_demo_queue_1")
                    .ticketNumber("GS-1043")
                    .salonId("salon_1")
                    .chairId("chair_1")
                    .customerName("Nuwan Perera")
                    .customerPhone("+94 71 234 5678")
                    .scheduledDate(today)
                    .scheduledTime("12:00")
                    .estimatedChairTime("12:00")
                    .totalDurationMinutes(45)
                    .totalPriceLkr(400.0)
                    .otp("7392")
                    .queuePosition(1)
                    .status(AppointmentStatus.PENDING)
                    .selectedServiceIdsJson("srv_1")
                    .createdAt(LocalDateTime.now().minusMinutes(35))
                    .build();

            // Upcoming booking for Chair 2
            Appointment upcoming2 = Appointment.builder()
                    .id("app_demo_queue_2")
                    .ticketNumber("GS-1044")
                    .salonId("salon_1")
                    .chairId("chair_2")
                    .customerName("Rizwan Ahamed")
                    .customerPhone("+94 76 555 4321")
                    .scheduledDate(today)
                    .scheduledTime("12:45")
                    .estimatedChairTime("12:45")
                    .totalDurationMinutes(45)
                    .totalPriceLkr(800.0)
                    .otp("3150")
                    .queuePosition(1)
                    .status(AppointmentStatus.PENDING)
                    .selectedServiceIdsJson("srv_2")
                    .createdAt(LocalDateTime.now().minusMinutes(10))
                    .build();

            appointmentRepository.saveAll(List.of(seated, upcoming1, upcoming2));
            log.info("Seeded initial demo appointments for queue simulation.");
        }

        log.info("Trimly database seeding completed successfully.");
    }
}
