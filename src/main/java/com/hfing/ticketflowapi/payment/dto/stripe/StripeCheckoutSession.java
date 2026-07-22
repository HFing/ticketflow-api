package com.hfing.ticketflowapi.payment.dto.stripe;

import lombok.Builder;

@Builder
public record StripeCheckoutSession(
        String providerSessionId,
        String providerPaymentId,
        String checkoutUrl
) {
}
