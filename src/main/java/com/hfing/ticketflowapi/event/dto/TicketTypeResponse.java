package com.hfing.ticketflowapi.event.dto;

import com.hfing.ticketflowapi.event.enums.TicketTypeStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record TicketTypeResponse(
        String id,
        String name,
        String description,
        BigDecimal price,
        Integer totalQuantity,
        Integer soldQuantity,
        Integer heldQuantity,
        Integer maxPerOrder,
        TicketTypeStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
