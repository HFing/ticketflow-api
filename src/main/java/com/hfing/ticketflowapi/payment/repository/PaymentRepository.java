package com.hfing.ticketflowapi.payment.repository;

import com.hfing.ticketflowapi.payment.entity.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"booking", "booking.eventShow", "booking.eventShow.event",
            "booking.customer", "booking.items", "booking.items.ticketType"})
    Optional<Payment> findByBookingId(String bookingId);
}
