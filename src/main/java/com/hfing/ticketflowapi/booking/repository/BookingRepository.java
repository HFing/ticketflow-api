package com.hfing.ticketflowapi.booking.repository;

import com.hfing.ticketflowapi.booking.dto.response.BookingSummaryResponse;
import com.hfing.ticketflowapi.booking.entity.Booking;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, String> {
    @Query("""
            SELECT new com.hfing.ticketflowapi.booking.dto.response.BookingSummaryResponse(
                b.id,
                b.eventShow.event.name,
                b.eventShow.id,
                b.eventShow.startTime,
                b.totalAmount,
                b.status,
                b.createdAt
            )
            FROM Booking b
            WHERE b.customer.id = :customerId
            ORDER BY b.createdAt DESC
            """)
    List<BookingSummaryResponse> findSummariesByCustomerId(@Param("customerId") String customerId);

    @EntityGraph(attributePaths = {"eventShow", "eventShow.event", "items", "items.ticketType"})
    Optional<Booking> findByIdAndCustomerId(String id, String customerId);

    @EntityGraph(attributePaths = {"eventShow", "eventShow.event", "items", "items.ticketType"})
    Optional<Booking> findByCustomerIdAndIdempotencyKey(String customerId, String idempotencyKey);
}
