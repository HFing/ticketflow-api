package com.hfing.ticketflowapi.event.service;

import com.hfing.ticketflowapi.event.dto.request.CreateEventRequest;
import com.hfing.ticketflowapi.event.dto.request.CreateEventShowRequest;
import com.hfing.ticketflowapi.event.dto.request.CreateTicketTypeRequest;
import com.hfing.ticketflowapi.event.dto.response.EventResponse;
import com.hfing.ticketflowapi.event.dto.response.EventShowResponse;
import com.hfing.ticketflowapi.event.dto.response.PublicEventResponse;
import com.hfing.ticketflowapi.event.dto.response.PublicEventSummaryResponse;
import com.hfing.ticketflowapi.event.dto.response.TicketTypeResponse;
import com.hfing.ticketflowapi.event.dto.request.UpdateEventRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface IEventService {

    EventResponse createEvent(
            CreateEventRequest request,
            MultipartFile shortImage,
            MultipartFile bannerImage);

    Page<PublicEventSummaryResponse> getPublishedUpcomingEvents(Pageable pageable);

    Page<EventResponse> getAllEventsForAdmin(Pageable pageable);

    EventResponse getAdminEventById(String id);

    PublicEventResponse getPublicEventById(String id);

    EventResponse updateEvent(String id, UpdateEventRequest request, String currentUserId, String role);

    void deleteEvent(String id, String currentUserId, String role);

    EventResponse cancelEvent(String id, String currentUserId, String role);

    EventShowResponse createShow(String eventId, CreateEventShowRequest request, String currentUserId, String role);

    TicketTypeResponse createTicketType(String showId, CreateTicketTypeRequest request, String currentUserId,
            String role);

    EventResponse submitForReview(String eventId, String currentUserId, String role);

    Page<EventResponse> getPendingEvents(Pageable pageable);

    EventResponse approveEvent(String eventId);

    EventResponse rejectEvent(String eventId);

    EventResponse setHotEvent(String eventId, boolean isHot);
}
