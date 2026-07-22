package com.hfing.ticketflowapi.payment.service;

import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.payment.config.PaymentProperties;
import com.hfing.ticketflowapi.payment.dto.stripe.StripeWebhookEvent;
import com.hfing.ticketflowapi.payment.enums.PaymentStatus;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class StripeWebhookService {
    private static final String BOOKING_ID_METADATA_KEY = "booking_id";

    private final PaymentProperties properties;
    private final PaymentTransactionService paymentTransactionService;

    public void process(String payload, String signature) {
        Event event = constructEvent(payload, signature);
        if (!isHandledEvent(event.getType())) {
            return;
        }

        Session session = checkoutSession(event);
        PaymentStatus status = mapStatus(event.getType(), session);
        validateSession(session, status);
        paymentTransactionService.applyStripeEvent(StripeWebhookEvent.builder()
                .bookingId(session.getMetadata().get(BOOKING_ID_METADATA_KEY))
                .providerSessionId(session.getId())
                .providerPaymentId(session.getPaymentIntent())
                .status(status)
                .amount(session.getAmountTotal())
                .currency(session.getCurrency())
                .occurredAt(Instant.ofEpochSecond(event.getCreated()))
                .build());
    }

    private Event constructEvent(String payload, String signature) {
        String webhookSecret = properties.stripe() == null
                ? null
                : properties.stripe().webhookSecret();
        if (!StringUtils.hasText(payload)
                || !StringUtils.hasText(signature)
                || !StringUtils.hasText(webhookSecret)) {
            throw new AppException(ErrorCode.PAYMENT_WEBHOOK_INVALID);
        }
        try {
            return Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException | RuntimeException exception) {
            throw new AppException(ErrorCode.PAYMENT_WEBHOOK_INVALID);
        }
    }

    private Session checkoutSession(Event event) {
        StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);
        if (!(stripeObject instanceof Session session) || session.getMetadata() == null) {
            throw new AppException(ErrorCode.PAYMENT_WEBHOOK_INVALID);
        }
        return session;
    }

    private PaymentStatus mapStatus(String eventType, Session session) {
        return switch (eventType) {
            case "checkout.session.completed" -> "paid".equals(session.getPaymentStatus())
                    ? PaymentStatus.PAID
                    : PaymentStatus.PROCESSING;
            case "checkout.session.async_payment_succeeded" -> PaymentStatus.PAID;
            case "checkout.session.async_payment_failed" -> PaymentStatus.FAILED;
            case "checkout.session.expired" -> PaymentStatus.EXPIRED;
            default -> null;
        };
    }

    private void validateSession(Session session, PaymentStatus status) {
        String bookingId = session.getMetadata().get(BOOKING_ID_METADATA_KEY);
        if (!StringUtils.hasText(bookingId)
                || !StringUtils.hasText(session.getId())
                || session.getAmountTotal() == null
                || !StringUtils.hasText(session.getCurrency())
                || (status == PaymentStatus.PAID
                        && !StringUtils.hasText(session.getPaymentIntent()))) {
            throw new AppException(ErrorCode.PAYMENT_WEBHOOK_INVALID);
        }
    }

    private boolean isHandledEvent(String eventType) {
        return switch (eventType) {
            case "checkout.session.completed",
                    "checkout.session.async_payment_succeeded",
                    "checkout.session.async_payment_failed",
                    "checkout.session.expired" -> true;
            default -> false;
        };
    }
}
