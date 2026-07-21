package com.hfing.ticketflowapi.booking.dto.response;

import com.hfing.ticketflowapi.booking.enums.TicketStatus;

import java.time.LocalDateTime;

public record OrganizerTicketResponse(
        String id,
        String ticketCode,
        TicketStatus status,
        LocalDateTime checkedInAt,
        String ticketTypeId,
        String ticketTypeName,
        String eventShowId,
        String customerId,
        String customerEmail
) {
}
