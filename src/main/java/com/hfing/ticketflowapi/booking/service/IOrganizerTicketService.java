package com.hfing.ticketflowapi.booking.service;

import com.hfing.ticketflowapi.booking.dto.request.CheckInTicketRequest;
import com.hfing.ticketflowapi.booking.dto.response.OrganizerTicketResponse;

import java.util.List;

public interface IOrganizerTicketService {
    OrganizerTicketResponse checkIn(String organizerId, CheckInTicketRequest request);

    List<OrganizerTicketResponse> getTicketsByEventShow(String organizerId, String showId);
}
