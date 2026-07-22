package com.hfing.ticketflowapi.payment.service;

import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.payment.config.PaymentProperties;
import com.hfing.ticketflowapi.payment.dto.stripe.StripeWebhookEvent;
import com.hfing.ticketflowapi.payment.enums.PaymentStatus;
import com.stripe.Stripe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {
    private static final String WEBHOOK_SECRET = "whsec_test_only";

    @Mock private PaymentTransactionService paymentTransactionService;
    private StripeWebhookService stripeWebhookService;

    @BeforeEach
    void setUp() {
        PaymentProperties properties = new PaymentProperties(
                "VND",
                Duration.ofMinutes(5),
                Duration.ofSeconds(30),
                100,
                new PaymentProperties.Stripe(
                        "sk_test_placeholder",
                        WEBHOOK_SECRET,
                        true,
                        "http://localhost/success?session_id={CHECKOUT_SESSION_ID}",
                        "http://localhost/cancel"));
        stripeWebhookService = new StripeWebhookService(properties, paymentTransactionService);
    }

    @Test
    void paidCheckoutWithValidSignatureIsForwardedForAtomicConfirmation() throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String payload = checkoutEventPayload("checkout.session.completed", "paid", timestamp);

        stripeWebhookService.process(payload, stripeSignature(payload, timestamp));

        ArgumentCaptor<StripeWebhookEvent> captor = ArgumentCaptor.forClass(StripeWebhookEvent.class);
        verify(paymentTransactionService).applyStripeEvent(captor.capture());
        StripeWebhookEvent event = captor.getValue();
        assertThat(event.bookingId()).isEqualTo("booking-1");
        assertThat(event.providerSessionId()).isEqualTo("cs_test_1");
        assertThat(event.providerPaymentId()).isEqualTo("pi_1");
        assertThat(event.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(event.amount()).isEqualTo(300000);
        assertThat(event.currency()).isEqualTo("vnd");
    }

    @Test
    void invalidSignatureIsRejectedBeforeDatabaseChanges() {
        assertThatThrownBy(() -> stripeWebhookService.process("{}", "t=1,v1=invalid"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_WEBHOOK_INVALID);

        verifyNoInteractions(paymentTransactionService);
    }

    @Test
    void completedButUnpaidCheckoutOnlyMovesPaymentToProcessing() throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String payload = checkoutEventPayload("checkout.session.completed", "unpaid", timestamp);

        stripeWebhookService.process(payload, stripeSignature(payload, timestamp));

        ArgumentCaptor<StripeWebhookEvent> captor = ArgumentCaptor.forClass(StripeWebhookEvent.class);
        verify(paymentTransactionService).applyStripeEvent(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PaymentStatus.PROCESSING);
    }

    private String checkoutEventPayload(String eventType, String paymentStatus, long timestamp) {
        return """
                {
                  "id": "evt_1",
                  "object": "event",
                  "api_version": "%s",
                  "created": %d,
                  "type": "%s",
                  "data": {
                    "object": {
                      "id": "cs_test_1",
                      "object": "checkout.session",
                      "amount_total": 300000,
                      "currency": "vnd",
                      "payment_intent": "pi_1",
                      "payment_status": "%s",
                      "metadata": {"booking_id": "booking-1"}
                    }
                  }
                }
                """.formatted(Stripe.API_VERSION, timestamp, eventType, paymentStatus);
    }

    private String stripeSignature(String payload, long timestamp) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = hmac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
        return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(digest);
    }
}
