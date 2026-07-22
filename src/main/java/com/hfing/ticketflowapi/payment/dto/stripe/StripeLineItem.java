package com.hfing.ticketflowapi.payment.dto.stripe;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record StripeLineItem(
        String name,
        BigDecimal unitAmount,
        long quantity
) {
}
