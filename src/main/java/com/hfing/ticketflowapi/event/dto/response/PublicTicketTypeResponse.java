package com.hfing.ticketflowapi.event.dto.response;

import com.hfing.ticketflowapi.event.enums.TicketTypeStatus;
import java.math.BigDecimal;

public record PublicTicketTypeResponse(
        String id,
        String name,
        String description,
        BigDecimal price,
        Integer totalQuantity,
        Integer maxPerOrder,
        TicketTypeStatus status
) {}
