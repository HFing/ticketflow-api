package com.hfing.ticketflowapi.booking.service;

import com.hfing.ticketflowapi.booking.dto.request.CheckInTicketRequest;
import com.hfing.ticketflowapi.booking.entity.Booking;
import com.hfing.ticketflowapi.booking.entity.Ticket;
import com.hfing.ticketflowapi.booking.enums.TicketStatus;
import com.hfing.ticketflowapi.booking.mapper.BookingMapper;
import com.hfing.ticketflowapi.booking.repository.TicketRepository;
import com.hfing.ticketflowapi.booking.service.impl.OrganizerTicketServiceImpl;
import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.event.entity.Event;
import com.hfing.ticketflowapi.event.entity.EventShow;
import com.hfing.ticketflowapi.event.entity.TicketType;
import com.hfing.ticketflowapi.event.enums.EventShowStatus;
import com.hfing.ticketflowapi.event.repository.EventShowRepository;
import com.hfing.ticketflowapi.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizerTicketServiceImplTest {
    @Mock private TicketRepository ticketRepository;
    @Mock private EventShowRepository eventShowRepository;
    @Spy private BookingMapper bookingMapper = Mappers.getMapper(BookingMapper.class);
    @InjectMocks private OrganizerTicketServiceImpl organizerTicketService;

    private EventShow eventShow;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        User organizer = User.builder().id("organizer-1").email("organizer@test.com").build();
        User customer = User.builder().id("customer-1").email("customer@test.com").build();
        Event event = Event.builder().id("event-1").organizer(organizer).build();
        eventShow = EventShow.builder()
                .id("show-1")
                .event(event)
                .status(EventShowStatus.ON_SALE)
                .build();
        TicketType ticketType = TicketType.builder()
                .id("type-1")
                .name("VIP")
                .eventShow(eventShow)
                .build();
        Booking booking = Booking.builder()
                .id("booking-1")
                .customer(customer)
                .eventShow(eventShow)
                .build();
        ticket = Ticket.builder()
                .id("ticket-1")
                .ticketCode("TFL-8D4F1A2B")
                .booking(booking)
                .ticketType(ticketType)
                .status(TicketStatus.VALID)
                .build();
    }

    @Test
    void checkIn_whenTicketIsValid_marksTicketAsUsed() {
        when(ticketRepository.findByTicketCodeForUpdate("TFL-8D4F1A2B"))
                .thenReturn(Optional.of(ticket));

        var response = organizerTicketService.checkIn(
                "organizer-1", new CheckInTicketRequest("TFL-8D4F1A2B"));

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
        assertThat(ticket.getCheckedInAt()).isNotNull();
        assertThat(response.status()).isEqualTo(TicketStatus.USED);
        assertThat(response.ticketTypeName()).isEqualTo("VIP");
        assertThat(response.customerEmail()).isEqualTo("customer@test.com");
    }

    @Test
    void checkIn_whenTicketWasAlreadyUsed_returnsTicketAlreadyUsed() {
        ticket.setStatus(TicketStatus.USED);
        ticket.setCheckedInAt(LocalDateTime.now().minusMinutes(1));
        when(ticketRepository.findByTicketCodeForUpdate("TFL-8D4F1A2B"))
                .thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> organizerTicketService.checkIn(
                "organizer-1", new CheckInTicketRequest("TFL-8D4F1A2B")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TICKET_ALREADY_USED);
    }

    @Test
    void checkIn_whenTicketBelongsToAnotherOrganizer_returnsForbidden() {
        when(ticketRepository.findByTicketCodeForUpdate("TFL-8D4F1A2B"))
                .thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> organizerTicketService.checkIn(
                "organizer-2", new CheckInTicketRequest("TFL-8D4F1A2B")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TICKET_FORBIDDEN_ACCESS);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.VALID);
    }

    @Test
    void checkIn_whenShowIsCancelled_returnsShowCancelled() {
        eventShow.setStatus(EventShowStatus.CANCELLED);
        when(ticketRepository.findByTicketCodeForUpdate("TFL-8D4F1A2B"))
                .thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> organizerTicketService.checkIn(
                "organizer-1", new CheckInTicketRequest("TFL-8D4F1A2B")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SHOW_CANCELLED);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.VALID);
    }

    @Test
    void getTicketsByEventShow_whenOrganizerOwnsShow_returnsTickets() {
        when(eventShowRepository.findById("show-1")).thenReturn(Optional.of(eventShow));
        when(ticketRepository.findAllByBookingEventShowIdOrderByCreatedAtAsc("show-1"))
                .thenReturn(List.of(ticket));

        var response = organizerTicketService.getTicketsByEventShow("organizer-1", "show-1");

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().ticketCode()).isEqualTo("TFL-8D4F1A2B");
        verify(ticketRepository).findAllByBookingEventShowIdOrderByCreatedAtAsc("show-1");
    }

    @Test
    void getTicketsByEventShow_whenOrganizerDoesNotOwnShow_doesNotReturnTickets() {
        when(eventShowRepository.findById("show-1")).thenReturn(Optional.of(eventShow));

        assertThatThrownBy(() -> organizerTicketService.getTicketsByEventShow("organizer-2", "show-1"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TICKET_FORBIDDEN_ACCESS);

        verify(ticketRepository, never()).findAllByBookingEventShowIdOrderByCreatedAtAsc("show-1");
    }
}
