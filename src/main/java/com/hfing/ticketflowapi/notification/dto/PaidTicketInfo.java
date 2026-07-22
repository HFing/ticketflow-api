package com.hfing.ticketflowapi.notification.dto;

public record PaidTicketInfo(
        String ticketCode,
        String ticketTypeName
) {
}
