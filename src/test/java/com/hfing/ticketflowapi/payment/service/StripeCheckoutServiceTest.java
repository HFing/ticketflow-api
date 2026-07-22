package com.hfing.ticketflowapi.payment.service;

import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.payment.config.PaymentProperties;
import com.hfing.ticketflowapi.payment.dto.stripe.CreateStripeCheckoutCommand;
import com.hfing.ticketflowapi.payment.dto.stripe.StripeLineItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StripeCheckoutServiceTest {
    private StripeCheckoutService stripeCheckoutService;

    @BeforeEach
    void setUp() {
        PaymentProperties properties = new PaymentProperties(
                "VND",
                Duration.ofMinutes(5),
                Duration.ofSeconds(30),
                100,
                new PaymentProperties.Stripe(
                        "sk_test_placeholder",
                        "whsec_test_only",
                        true,
                        "http://localhost/success?session_id={CHECKOUT_SESSION_ID}",
                        "http://localhost/cancel"));
        stripeCheckoutService = new StripeCheckoutService(properties);
    }

    @Test
    void checkoutIdempotencyKeyIsStablePerBooking() {
        assertThat(stripeCheckoutService.checkoutRequestOptions("booking-1").getIdempotencyKey())
                .isEqualTo("checkout-session:booking-1");
    }

    @Test
    void checkoutRejectsAlreadyExpiredBooking() {
        CreateStripeCheckoutCommand command = CreateStripeCheckoutCommand.builder()
                .bookingId("booking-1")
                .amount(new BigDecimal("150000"))
                .currency("VND")
                .customerEmail("customer@example.com")
                .expiresAt(Instant.now().minusSeconds(1))
                .items(List.of(StripeLineItem.builder()
                        .name("VIP")
                        .unitAmount(new BigDecimal("150000"))
                        .quantity(1)
                        .build()))
                .build();

        assertThatThrownBy(() -> stripeCheckoutService.createSession(command))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_INITIALIZATION_FAILED);
    }
}
