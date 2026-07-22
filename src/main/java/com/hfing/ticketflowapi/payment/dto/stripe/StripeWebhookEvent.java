package com.hfing.ticketflowapi.payment.dto.stripe;

import com.hfing.ticketflowapi.payment.enums.PaymentStatus;
import lombok.Builder;

import java.time.Instant;

@Builder
public record StripeWebhookEvent(
        String bookingId,
        String providerSessionId,
        String providerPaymentId,
        PaymentStatus status,
        long amount,
        String currency,
        Instant occurredAt
) {
}
