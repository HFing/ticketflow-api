package com.hfing.ticketflowapi.payment.repository;

import com.hfing.ticketflowapi.payment.entity.Payment;
import com.hfing.ticketflowapi.booking.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    @EntityGraph(attributePaths = {
            "booking", "booking.eventShow", "booking.items", "booking.items.ticketType"
    })
    Optional<Payment> findByBookingId(String bookingId);

    @EntityGraph(attributePaths = {
            "booking", "booking.items", "booking.items.ticketType"
    })
    Optional<Payment> findByBookingIdAndBookingCustomerId(String bookingId, String customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :paymentId")
    Optional<Payment> findByIdForUpdate(@Param("paymentId") String paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p FROM Payment p
            WHERE p.providerSessionId = :providerSessionId
            """)
    Optional<Payment> findByProviderSessionIdForUpdate(
            @Param("providerSessionId") String providerSessionId);

    @Query("""
            SELECT p.id FROM Payment p
            WHERE p.booking.status = :bookingStatus
              AND p.booking.expiresAt <= :now
            ORDER BY p.booking.expiresAt ASC
            """)
    List<String> findExpiredPaymentIds(
            @Param("bookingStatus") BookingStatus bookingStatus,
            @Param("now") Instant now,
            Pageable pageable);
}
