package com.hfing.ticketflowapi.event.service;

import com.hfing.ticketflowapi.event.dto.CreateEventRequest;
import com.hfing.ticketflowapi.event.dto.CreateEventShowRequest;
import com.hfing.ticketflowapi.event.dto.CreateTicketTypeRequest;
import com.hfing.ticketflowapi.event.dto.EventResponse;
import com.hfing.ticketflowapi.event.dto.EventShowResponse;
import com.hfing.ticketflowapi.event.dto.TicketTypeResponse;
import com.hfing.ticketflowapi.event.dto.UpdateEventRequest;
import java.util.List;

public interface EventService {

    EventResponse createEvent(CreateEventRequest request);

    List<EventResponse> getPublishedUpcomingEvents();

    List<EventResponse> getAllEventsForAdmin();

    EventResponse getAdminEventById(String id);

    EventResponse getPublicEventById(String id);

    EventResponse updateEvent(String id, UpdateEventRequest request, String currentUserId, String role);

    void deleteEvent(String id, String currentUserId, String role);

    EventResponse cancelEvent(String id, String currentUserId, String role);

    EventShowResponse createShow(String eventId, CreateEventShowRequest request, String currentUserId, String role);

    TicketTypeResponse createTicketType(String showId, CreateTicketTypeRequest request, String currentUserId,
            String role);

    EventResponse submitForReview(String eventId, String currentUserId, String role);

    List<EventResponse> getPendingEvents();

    EventResponse approveEvent(String eventId);

    EventResponse rejectEvent(String eventId);
}