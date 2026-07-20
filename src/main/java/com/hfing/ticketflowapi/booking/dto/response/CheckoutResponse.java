package com.hfing.ticketflowapi.booking.dto.response;

import com.hfing.ticketflowapi.booking.enums.BookingStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record CheckoutResponse(
        String bookingId,
        String eventShowId,
        BigDecimal totalAmount,
        BookingStatus status,
        List<BookingItemResponse> items,
        PaymentResponse payment,
        List<TicketResponse> tickets
) {
}
