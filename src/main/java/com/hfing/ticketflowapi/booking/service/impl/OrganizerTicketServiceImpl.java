package com.hfing.ticketflowapi.booking.service.impl;

import com.hfing.ticketflowapi.booking.dto.request.CheckInTicketRequest;
import com.hfing.ticketflowapi.booking.dto.response.OrganizerTicketResponse;
import com.hfing.ticketflowapi.booking.entity.Ticket;
import com.hfing.ticketflowapi.booking.enums.TicketStatus;
import com.hfing.ticketflowapi.booking.mapper.BookingMapper;
import com.hfing.ticketflowapi.booking.repository.TicketRepository;
import com.hfing.ticketflowapi.booking.service.IOrganizerTicketService;
import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.event.entity.EventShow;
import com.hfing.ticketflowapi.event.enums.EventShowStatus;
import com.hfing.ticketflowapi.event.repository.EventShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizerTicketServiceImpl implements IOrganizerTicketService {
    private final TicketRepository ticketRepository;
    private final EventShowRepository eventShowRepository;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional
    public OrganizerTicketResponse checkIn(String organizerId, CheckInTicketRequest request) {
        Ticket ticket = ticketRepository.findByTicketCodeForUpdate(request.ticketCode().trim())
                .orElseThrow(() -> new AppException(ErrorCode.TICKET_NOT_FOUND));

        EventShow eventShow = ticket.getBooking().getEventShow();
        verifyOwnership(eventShow, organizerId);

        if (ticket.getStatus() == TicketStatus.USED || ticket.getCheckedInAt() != null) {
            throw new AppException(ErrorCode.TICKET_ALREADY_USED);
        }
        if (ticket.getStatus() != TicketStatus.VALID) {
            throw new AppException(ErrorCode.TICKET_NOT_VALID);
        }
        if (eventShow.getStatus() == EventShowStatus.CANCELLED) {
            throw new AppException(ErrorCode.SHOW_CANCELLED);
        }

        ticket.setStatus(TicketStatus.USED);
        ticket.setCheckedInAt(LocalDateTime.now());

        return bookingMapper.toOrganizerTicketResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrganizerTicketResponse> getTicketsByEventShow(
            String organizerId,
            String showId,
            Pageable pageable) {
        EventShow eventShow = eventShowRepository.findById(showId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_FOUND));
        verifyOwnership(eventShow, organizerId);

        return ticketRepository
                .findAllByBookingEventShowIdOrderByCreatedAtAsc(showId, pageable)
                .map(bookingMapper::toOrganizerTicketResponse);
    }

    private void verifyOwnership(EventShow eventShow, String organizerId) {
        if (!eventShow.getEvent().getOrganizer().getId().equals(organizerId)) {
            throw new AppException(ErrorCode.TICKET_FORBIDDEN_ACCESS);
        }
    }

}
