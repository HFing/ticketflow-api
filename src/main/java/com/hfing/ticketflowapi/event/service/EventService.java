package com.hfing.ticketflowapi.event.service;

import com.hfing.ticketflowapi.event.dto.CreateEventRequest;
import com.hfing.ticketflowapi.event.dto.EventResponse;
import com.hfing.ticketflowapi.event.dto.UpdateEventRequest;
import java.util.List;



public interface EventService {
    EventResponse createEvent(CreateEventRequest request, String currentUserId);
    List<EventResponse> getEvents(String currentUserId, String role);
    EventResponse getEventById(String id, String currentUserId, String role);
    EventResponse updateEvent(String id, UpdateEventRequest request, String currentUserId, String role);
    void deleteEvent(String id, String currentUserId, String role);
    EventResponse publishEvent(String id, String currentUserId, String role);
    EventResponse cancelEvent(String id, String currentUserId, String role);
    EventResponse getEventDetailFromCache(String id);
}