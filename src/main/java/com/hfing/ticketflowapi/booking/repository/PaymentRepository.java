package com.hfing.ticketflowapi.booking.repository;

import com.hfing.ticketflowapi.booking.dto.response.PaymentResponse;
import com.hfing.ticketflowapi.booking.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    @Query("""
            SELECT new com.hfing.ticketflowapi.booking.dto.response.PaymentResponse(
                p.id,
                p.amount,
                p.method,
                p.status,
                p.transactionCode,
                p.paidAt
            )
            FROM Payment p
            WHERE p.booking.id = :bookingId
            """)
    Optional<PaymentResponse> findResponseByBookingId(@Param("bookingId") String bookingId);
}
