package com.hfing.ticketflowapi.booking.dto.response;

import com.hfing.ticketflowapi.booking.enums.BookingStatus;
import com.hfing.ticketflowapi.payment.dto.response.CheckoutPaymentResponse;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder
public record CheckoutResponse(
        String bookingId,
        String eventShowId,
        BigDecimal totalAmount,
        BookingStatus status,
        List<BookingItemResponse> items,
        CheckoutPaymentResponse payment,
        Instant expiresAt,
        List<TicketResponse> tickets
) {
}
