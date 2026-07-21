package com.hfing.ticketflowapi.booking.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CheckInTicketRequest(
        @NotBlank(message = "Ticket code is required")
        String ticketCode
) {
}
