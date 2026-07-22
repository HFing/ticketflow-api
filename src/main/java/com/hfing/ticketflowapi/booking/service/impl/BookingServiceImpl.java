package com.hfing.ticketflowapi.booking.service.impl;

import com.hfing.ticketflowapi.booking.dto.request.CheckoutRequest;
import com.hfing.ticketflowapi.booking.dto.response.BookingDetailResponse;
import com.hfing.ticketflowapi.booking.dto.response.BookingSummaryResponse;
import com.hfing.ticketflowapi.booking.dto.response.CheckoutResponse;
import com.hfing.ticketflowapi.booking.entity.Booking;
import com.hfing.ticketflowapi.booking.mapper.BookingMapper;
import com.hfing.ticketflowapi.booking.repository.BookingRepository;
import com.hfing.ticketflowapi.booking.service.IBookingService;
import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.payment.dto.stripe.CreateStripeCheckoutCommand;
import com.hfing.ticketflowapi.payment.dto.stripe.StripeCheckoutSession;
import com.hfing.ticketflowapi.payment.dto.internal.PaymentReservation;
import com.hfing.ticketflowapi.payment.dto.internal.PaymentSessionReference;
import com.hfing.ticketflowapi.payment.dto.response.PaymentResponse;
import com.hfing.ticketflowapi.payment.entity.Payment;
import com.hfing.ticketflowapi.payment.mapper.PaymentMapper;
import com.hfing.ticketflowapi.payment.repository.PaymentRepository;
import com.hfing.ticketflowapi.payment.service.PaymentTransactionService;
import com.hfing.ticketflowapi.payment.service.StripeCheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements IBookingService {
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 255;

    private final PaymentTransactionService paymentTransactionService;
    private final StripeCheckoutService stripeCheckoutService;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final BookingMapper bookingMapper;
    private final PaymentMapper paymentMapper;

    @Override
    public CheckoutResponse checkout(
            String customerId,
            CheckoutRequest request,
            String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        String requestFingerprint = fingerprint(request);
        PaymentReservation reservation;
        try {
            reservation = paymentTransactionService.reserve(
                    customerId,
                    request,
                    idempotencyKey,
                    requestFingerprint);
        } catch (DataIntegrityViolationException exception) {
            reservation = paymentTransactionService.findExistingReservation(
                            customerId, idempotencyKey, requestFingerprint)
                    .orElseThrow(() -> exception);
        }

        StripeCheckoutSession session = reservation.providerSessionId() == null
                ? stripeCheckoutService.createSession(CreateStripeCheckoutCommand.builder()
                        .bookingId(reservation.bookingId())
                        .amount(reservation.amount())
                        .currency(reservation.currency())
                        .customerEmail(reservation.customerEmail())
                        .expiresAt(reservation.expiresAt())
                        .items(reservation.items())
                        .build())
                : stripeCheckoutService.retrieveSession(reservation.providerSessionId());

        try {
            return paymentTransactionService.attachGatewayPayment(reservation.paymentId(), session);
        } catch (AppException exception) {
            if (exception.getErrorCode() == ErrorCode.PAYMENT_INVALID_STATE) {
                stripeCheckoutService.expireSession(session.providerSessionId());
            }
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingSummaryResponse> getMyBookings(String customerId) {
        return bookingRepository.findSummariesByCustomerId(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDetailResponse getMyBookingDetail(String customerId, String bookingId) {
        Booking booking = bookingRepository.findByIdAndCustomerId(bookingId, customerId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        Payment paymentEntity = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        PaymentResponse payment = paymentMapper.toResponse(paymentEntity);

        return bookingMapper.toBookingDetailResponse(booking, payment);
    }

    @Override
    public void cancelBooking(String customerId, String bookingId) {
        PaymentSessionReference candidate = paymentTransactionService
                .getCancellationCandidate(customerId, bookingId);
        if (candidate.providerSessionId() != null) {
            stripeCheckoutService.expireSession(candidate.providerSessionId());
        }
        paymentTransactionService.cancel(candidate.paymentId());
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)
                || idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new AppException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }
    }

    private String fingerprint(CheckoutRequest request) {
        StringBuilder canonicalRequest = new StringBuilder(request.eventShowId());
        request.items().stream()
                .sorted((left, right) -> left.ticketTypeId().compareTo(right.ticketTypeId()))
                .forEach(item -> canonicalRequest
                        .append('|')
                        .append(item.ticketTypeId())
                        .append(':')
                        .append(item.quantity()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalRequest.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
