package com.hfing.ticketflowapi.payment.scheduler;

import com.hfing.ticketflowapi.booking.enums.BookingStatus;
import com.hfing.ticketflowapi.payment.config.PaymentProperties;
import com.hfing.ticketflowapi.payment.service.PaymentTransactionService;
import com.hfing.ticketflowapi.payment.service.StripeCheckoutService;
import com.hfing.ticketflowapi.payment.repository.PaymentRepository;
import com.hfing.ticketflowapi.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentExpirationScheduler {
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionService paymentTransactionService;
    private final StripeCheckoutService stripeCheckoutService;
    private final PaymentProperties paymentProperties;
    private final Clock paymentClock;

    @Scheduled(fixedDelayString = "${payment.expiration-scan-interval}")
    public void expireHeldInventory() {
        Instant now = paymentClock.instant();
        for (String paymentId : paymentRepository.findExpiredPaymentIds(
                BookingStatus.PENDING_PAYMENT,
                now,
                PageRequest.of(0, paymentProperties.expirationBatchSize()))) {
            expireOne(paymentId, now);
        }
    }

    private void expireOne(String paymentId, Instant now) {
        try {
            paymentTransactionService.getExpirationCandidate(paymentId, now)
                    .ifPresent(candidate -> {
                        if (candidate.providerSessionId() != null) {
                            stripeCheckoutService.expireSession(candidate.providerSessionId());
                        }
                        paymentTransactionService.expire(candidate.paymentId(), now);
                    });
        } catch (AppException exception) {
            log.warn("Payment expiration will be retried for paymentId={}", paymentId);
        }
    }
}
