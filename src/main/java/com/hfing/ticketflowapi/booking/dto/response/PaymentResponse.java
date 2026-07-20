package com.hfing.ticketflowapi.booking.dto.response;

import com.hfing.ticketflowapi.booking.enums.PaymentMethod;
import com.hfing.ticketflowapi.booking.enums.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PaymentResponse(
        String id,
        BigDecimal amount,
        PaymentMethod method,
        PaymentStatus status,
        String transactionCode,
        LocalDateTime paidAt
) {
}
