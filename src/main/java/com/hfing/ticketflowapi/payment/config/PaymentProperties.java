package com.hfing.ticketflowapi.payment.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "payment")
@Validated
public record PaymentProperties(
        @Pattern(regexp = "(?i)VND", message = "Payment currency must be VND")
        String currency,
        @NotNull
        Duration holdDuration,
        @NotNull
        Duration expirationScanInterval,
        @Min(1)
        int expirationBatchSize,
        Stripe stripe
) {
    public record Stripe(
            String secretKey,
            String webhookSecret,
            boolean testModeOnly,
            String successUrl,
            String cancelUrl) {
    }
}
