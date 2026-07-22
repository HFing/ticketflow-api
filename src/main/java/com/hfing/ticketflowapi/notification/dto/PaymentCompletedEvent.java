package com.hfing.ticketflowapi.notification.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentCompletedEvent(
        String paymentId,
        String bookingId,
        String customerEmail,
        String customerFirstName,
        String customerLastName,
        String eventName,
        String eventLocation,
        String eventVenue,
        LocalDateTime showStartTime,
        LocalDateTime showEndTime,
        BigDecimal totalAmount,
        String currency,
        Instant paidAt,
        List<PaidTicketInfo> tickets
) {
}
