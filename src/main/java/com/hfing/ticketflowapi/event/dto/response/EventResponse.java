package com.hfing.ticketflowapi.event.dto.response;

import com.hfing.ticketflowapi.event.enums.EventCategory;
import com.hfing.ticketflowapi.event.enums.EventStatus;
import java.time.Instant;


public record EventResponse(
        String id,
        String name,
        String description,
        String location,
        String venue,
        EventStatus status,
        String organizerId,
        String bannerUrl,
        String shortImageUrl,
        Boolean isHot,
        EventCategory category,
        Instant createdAt,
        Instant updatedAt
) {}
