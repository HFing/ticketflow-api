package com.hfing.ticketflowapi.payment.dto.internal;

import lombok.Builder;

@Builder
public record PaymentSessionReference(
        String paymentId,
        String providerSessionId
) {
}
