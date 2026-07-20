package com.hfing.ticketflowapi.booking.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record BookingItemResponse(
        String id,
        String ticketTypeId,
        String ticketTypeName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
