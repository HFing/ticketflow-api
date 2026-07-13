package com.hfing.ticketflowapi.dto.response;

import com.hfing.ticketflowapi.common.enums.EventStatus;
import com.hfing.ticketflowapi.common.enums.EventCategory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record EventResponse(
        String id,
        String name,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
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
