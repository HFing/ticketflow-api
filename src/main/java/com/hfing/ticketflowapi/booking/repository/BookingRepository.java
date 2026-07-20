package com.hfing.ticketflowapi.booking.repository;

import com.hfing.ticketflowapi.booking.entity.Booking;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, String> {
    @EntityGraph(attributePaths = {"eventShow", "eventShow.event"})
    List<Booking> findAllByCustomerIdOrderByCreatedAtDesc(String customerId);

    @EntityGraph(attributePaths = {"eventShow", "eventShow.event", "items", "items.ticketType"})
    Optional<Booking> findByIdAndCustomerId(String id, String customerId);
}
