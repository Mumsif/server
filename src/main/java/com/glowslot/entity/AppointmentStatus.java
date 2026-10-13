package com.glowslot.entity;

/**
 * Lifecycle states of a salon appointment in the Trimly queue system:
 * - PENDING: Customer booked online or walk-in registered, waiting in queue.
 * - SEATED: Customer arrived at salon, OTP was validated by barber, customer is seated in chair.
 * - IN_SERVICE: Active haircut/styling currently underway.
 * - COMPLETED: Service done, barber clicked complete, LKR 50 commission deducted.
 * - CANCELLED: Customer cancelled booking prior to service.
 * - NO_SHOW: Customer did not arrive during their window.
 */
public enum AppointmentStatus {
    PENDING,
    SEATED,
    IN_SERVICE,
    COMPLETED,
    CANCELLED,
    NO_SHOW
}
