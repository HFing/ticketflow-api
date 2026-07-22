package com.hfing.ticketflowapi.booking.dto.response;

import com.hfing.ticketflowapi.booking.enums.BookingStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record BookingDetailResponse(
                String id,
                String eventName,
                String location,
                LocalDateTime showStartTime,
                LocalDateTime showEndTime,
                BigDecimal totalAmount,
                BookingStatus status,
                List<BookingItemResponse> items,
                Instant createdAt) {
}
