package com.hfing.ticketflowapi.booking.repository;

import com.hfing.ticketflowapi.booking.entity.Ticket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "booking.eventShow.event.organizer",
            "booking.customer",
            "ticketType"
    })
    @Query("SELECT t FROM Ticket t WHERE t.ticketCode = :ticketCode")
    Optional<Ticket> findByTicketCodeForUpdate(@Param("ticketCode") String ticketCode);

    @EntityGraph(attributePaths = {"booking.customer", "ticketType"})
    List<Ticket> findAllByBookingEventShowIdOrderByCreatedAtAsc(String eventShowId);
}
