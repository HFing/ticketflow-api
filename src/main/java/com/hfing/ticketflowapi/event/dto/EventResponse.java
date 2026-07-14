package com.hfing.ticketflowapi.event.dto;

import com.hfing.ticketflowapi.event.enums.EventCategory;
import com.hfing.ticketflowapi.event.enums.EventStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;


public record EventResponse(
        String id,
        String name,
        String description,
        String location,
        EventStatus status,
        String organizerId,
        String bannerUrl,
        String shortImageUrl,
        EventCategory category,
        List<EventShowResponse> shows,
        Instant createdAt,
        Instant updatedAt
) {}