package com.hfing.ticketflowapi.booking.service;

import com.hfing.ticketflowapi.booking.dto.request.CheckInTicketRequest;
import com.hfing.ticketflowapi.booking.dto.response.OrganizerTicketResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IOrganizerTicketService {
    OrganizerTicketResponse checkIn(String organizerId, CheckInTicketRequest request);

    Page<OrganizerTicketResponse> getTicketsByEventShow(
            String organizerId,
            String showId,
            Pageable pageable);
}
