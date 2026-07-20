package com.hfing.ticketflowapi.booking.entity;

import com.hfing.ticketflowapi.booking.enums.TicketStatus;
import com.hfing.ticketflowapi.common.entity.BaseEntity;
import com.hfing.ticketflowapi.event.entity.TicketType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets", indexes = {
        @Index(name = "idx_tickets_booking_id", columnList = "booking_id"),
        @Index(name = "idx_tickets_ticket_code", columnList = "ticket_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_id", nullable = false)
    private TicketType ticketType;

    @Column(name = "ticket_code", nullable = false, unique = true)
    private String ticketCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;
}
