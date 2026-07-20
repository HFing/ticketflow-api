package com.hfing.ticketflowapi.booking.dto.response;

import com.hfing.ticketflowapi.booking.enums.BookingStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Builder
public record BookingSummaryResponse(
        String id,
        String eventName,
        String eventShowId,
        LocalDateTime showStartTime,
        BigDecimal totalAmount,
        BookingStatus status,
        Instant createdAt
) {
}
