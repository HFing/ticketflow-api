package com.hfing.ticketflowapi.payment.scheduler;

import com.hfing.ticketflowapi.booking.enums.BookingStatus;
import com.hfing.ticketflowapi.payment.config.PaymentProperties;
import com.hfing.ticketflowapi.payment.dto.internal.PaymentSessionReference;
import com.hfing.ticketflowapi.payment.repository.PaymentRepository;
import com.hfing.ticketflowapi.payment.service.PaymentTransactionService;
import com.hfing.ticketflowapi.payment.service.StripeCheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentExpirationSchedulerTest {
    private static final Instant NOW = Instant.parse("2026-07-22T03:00:00Z");

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentTransactionService paymentTransactionService;
    @Mock private StripeCheckoutService stripeCheckoutService;

    private PaymentExpirationScheduler scheduler;

    @BeforeEach
    void setUp() {
        PaymentProperties properties = new PaymentProperties(
                "VND",
                Duration.ofMinutes(5),
                Duration.ofSeconds(5),
                100,
                new PaymentProperties.Stripe(
                        "sk_test_placeholder",
                        "whsec_test_only",
                        true,
                        "http://localhost/success",
                        "http://localhost/cancel"));
        scheduler = new PaymentExpirationScheduler(
                paymentRepository,
                paymentTransactionService,
                stripeCheckoutService,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void expiredBookingExpiresStripeSessionBeforeReleasingHeldInventory() {
        when(paymentRepository.findExpiredPaymentIds(
                BookingStatus.PENDING_PAYMENT,
                NOW,
                PageRequest.of(0, 100)))
                .thenReturn(List.of("payment-1"));
        when(paymentTransactionService.getExpirationCandidate("payment-1", NOW))
                .thenReturn(Optional.of(PaymentSessionReference.builder()
                        .paymentId("payment-1")
                        .providerSessionId("cs_test_1")
                        .build()));

        scheduler.expireHeldInventory();

        InOrder inOrder = inOrder(stripeCheckoutService, paymentTransactionService);
        inOrder.verify(stripeCheckoutService).expireSession("cs_test_1");
        inOrder.verify(paymentTransactionService).expire("payment-1", NOW);
    }

    @Test
    void expiredBookingWithoutStripeSessionStillReleasesHeldInventory() {
        when(paymentRepository.findExpiredPaymentIds(
                BookingStatus.PENDING_PAYMENT,
                NOW,
                PageRequest.of(0, 100)))
                .thenReturn(List.of("payment-1"));
        when(paymentTransactionService.getExpirationCandidate("payment-1", NOW))
                .thenReturn(Optional.of(PaymentSessionReference.builder()
                        .paymentId("payment-1")
                        .build()));

        scheduler.expireHeldInventory();

        verify(paymentTransactionService).expire("payment-1", NOW);
    }
}
