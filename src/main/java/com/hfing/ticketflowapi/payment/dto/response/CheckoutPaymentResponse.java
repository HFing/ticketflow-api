package com.hfing.ticketflowapi.payment.dto.response;

import com.hfing.ticketflowapi.payment.enums.PaymentProvider;
import com.hfing.ticketflowapi.payment.enums.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CheckoutPaymentResponse(
        String id,
        BigDecimal amount,
        String currency,
        PaymentProvider provider,
        PaymentStatus status,
        String providerSessionId,
        String providerPaymentId,
        String checkoutUrl
) {
}
