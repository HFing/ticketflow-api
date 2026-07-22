package com.hfing.ticketflowapi.payment.dto.stripe;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder
public record CreateStripeCheckoutCommand(
        String bookingId,
        BigDecimal amount,
        String currency,
        String customerEmail,
        Instant expiresAt,
        List<StripeLineItem> items
) {
}
