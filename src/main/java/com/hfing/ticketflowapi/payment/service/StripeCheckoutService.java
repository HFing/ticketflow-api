package com.hfing.ticketflowapi.payment.service;

import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.payment.config.PaymentProperties;
import com.hfing.ticketflowapi.payment.dto.stripe.CreateStripeCheckoutCommand;
import com.hfing.ticketflowapi.payment.dto.stripe.StripeCheckoutSession;
import com.hfing.ticketflowapi.payment.dto.stripe.StripeLineItem;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionExpireParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StripeCheckoutService {
    private static final String BOOKING_ID_METADATA_KEY = "booking_id";
    private static final Duration STRIPE_SESSION_DURATION = Duration.ofMinutes(31);

    private final PaymentProperties properties;

    public StripeCheckoutSession createSession(CreateStripeCheckoutCommand command) {
        validateCheckoutRequest(command);
        try {
            SessionCreateParams.Builder params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(stripeProperties().successUrl())
                    .setCancelUrl(stripeProperties().cancelUrl())
                    .setClientReferenceId(command.bookingId())
                    .setCustomerEmail(command.customerEmail())
                    .setExpiresAt(Instant.now().plus(STRIPE_SESSION_DURATION).getEpochSecond())
                    .putMetadata(BOOKING_ID_METADATA_KEY, command.bookingId())
                    .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
                            .putMetadata(BOOKING_ID_METADATA_KEY, command.bookingId())
                            .build());

            command.items().forEach(item -> params.addLineItem(toStripeLineItem(item, command.currency())));
            RequestOptions requestOptions = checkoutRequestOptions(command.bookingId());
            Session session = client().v1().checkout().sessions().create(params.build(), requestOptions);
            return toResult(session);
        } catch (StripeException | ArithmeticException exception) {
            throw new AppException(ErrorCode.PAYMENT_INITIALIZATION_FAILED);
        }
    }

    public StripeCheckoutSession retrieveSession(String providerSessionId) {
        try {
            return toResult(client().v1().checkout().sessions().retrieve(providerSessionId));
        } catch (StripeException exception) {
            throw new AppException(ErrorCode.PAYMENT_INITIALIZATION_FAILED);
        }
    }

    public void expireSession(String providerSessionId) {
        try {
            RequestOptions requestOptions = RequestOptions.builder()
                    .setIdempotencyKey("expire-checkout-session:" + providerSessionId)
                    .build();
            client().v1().checkout().sessions().expire(
                    providerSessionId,
                    SessionExpireParams.builder().build(),
                    requestOptions);
        } catch (StripeException exception) {
            throw new AppException(ErrorCode.PAYMENT_INITIALIZATION_FAILED);
        }
    }

    private SessionCreateParams.LineItem toStripeLineItem(StripeLineItem item, String currency) {
        return SessionCreateParams.LineItem.builder()
                .setQuantity(item.quantity())
                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(currency.toLowerCase(Locale.ROOT))
                        .setUnitAmount(item.unitAmount().longValueExact())
                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                .setName(item.name())
                                .build())
                        .build())
                .build();
    }

    private void validateCheckoutRequest(CreateStripeCheckoutCommand command) {
        PaymentProperties.Stripe stripe = stripeProperties();
        if (!StringUtils.hasText(stripe.successUrl())
                || !StringUtils.hasText(stripe.cancelUrl())
                || !StringUtils.hasText(command.bookingId())
                || !StringUtils.hasText(command.currency())
                || !StringUtils.hasText(command.customerEmail())
                || command.amount() == null
                || command.items() == null
                || command.items().isEmpty()
                || command.expiresAt() == null
                || !command.expiresAt().isAfter(Instant.now())
                || command.items().stream().anyMatch(item -> item == null
                        || !StringUtils.hasText(item.name())
                        || item.unitAmount() == null
                        || item.quantity() <= 0)) {
            throw new AppException(ErrorCode.PAYMENT_INITIALIZATION_FAILED);
        }

        BigDecimal lineItemTotal = command.items().stream()
                .map(item -> item.unitAmount().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (command.amount().compareTo(lineItemTotal) != 0) {
            throw new AppException(ErrorCode.PAYMENT_DETAILS_MISMATCH);
        }
    }

    private StripeClient client() {
        String secretKey = stripeProperties().secretKey();
        if (!StringUtils.hasText(secretKey)) {
            throw new AppException(ErrorCode.PAYMENT_INITIALIZATION_FAILED);
        }
        if (stripeProperties().testModeOnly()
                && !secretKey.startsWith("sk_test_")
                && !secretKey.startsWith("rk_test_")) {
            throw new AppException(ErrorCode.PAYMENT_INITIALIZATION_FAILED);
        }
        return new StripeClient(secretKey);
    }

    RequestOptions checkoutRequestOptions(String bookingId) {
        return RequestOptions.builder()
                .setIdempotencyKey("checkout-session:" + bookingId)
                .build();
    }

    private PaymentProperties.Stripe stripeProperties() {
        if (properties.stripe() == null) {
            throw new AppException(ErrorCode.PAYMENT_INITIALIZATION_FAILED);
        }
        return properties.stripe();
    }

    private StripeCheckoutSession toResult(Session session) {
        return StripeCheckoutSession.builder()
                .providerSessionId(session.getId())
                .providerPaymentId(session.getPaymentIntent())
                .checkoutUrl(session.getUrl())
                .build();
    }
}
