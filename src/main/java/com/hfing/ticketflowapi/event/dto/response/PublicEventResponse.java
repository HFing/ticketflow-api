package com.hfing.ticketflowapi.event.dto.response;

import com.hfing.ticketflowapi.event.enums.EventCategory;
import java.math.BigDecimal;
import java.util.List;

public record PublicEventResponse(
        String id,
        String name,
        String description,
        String location,
        String venue,
        String organizerName,
        String bannerUrl,
        String shortImageUrl,
        Boolean isHot,
        EventCategory category,
        BigDecimal minPrice,
        List<PublicEventShowResponse> shows
) {}
