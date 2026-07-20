package com.hfing.ticketflowapi.booking.dto.response;

import com.hfing.ticketflowapi.booking.enums.TicketStatus;
import lombok.Builder;

@Builder
public record TicketResponse(
        String id,
        String ticketTypeId,
        String ticketTypeName,
        String ticketCode,
        TicketStatus status
) {
}
