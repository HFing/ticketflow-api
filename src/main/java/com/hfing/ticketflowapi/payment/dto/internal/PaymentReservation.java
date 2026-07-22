package com.hfing.ticketflowapi.payment.dto.internal;

import com.hfing.ticketflowapi.payment.dto.stripe.StripeLineItem;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder
public record PaymentReservation(
        String paymentId,
        String bookingId,
        BigDecimal amount,
        String currency,
        String providerSessionId,
        String customerEmail,
        Instant expiresAt,
        List<StripeLineItem> items,
        String providerPaymentId
) {
}
