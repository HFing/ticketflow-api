package com.hfing.ticketflowapi.booking.service;

import com.hfing.ticketflowapi.booking.dto.request.CheckoutItemRequest;
import com.hfing.ticketflowapi.booking.dto.request.CheckoutRequest;
import com.hfing.ticketflowapi.booking.dto.response.BookingSummaryResponse;
import com.hfing.ticketflowapi.booking.dto.response.CheckoutResponse;
import com.hfing.ticketflowapi.booking.entity.Booking;
import com.hfing.ticketflowapi.booking.enums.BookingStatus;
import com.hfing.ticketflowapi.booking.mapper.BookingMapper;
import com.hfing.ticketflowapi.booking.repository.BookingRepository;
import com.hfing.ticketflowapi.booking.service.impl.BookingServiceImpl;
import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.event.entity.Event;
import com.hfing.ticketflowapi.event.entity.EventShow;
import com.hfing.ticketflowapi.event.enums.EventStatus;
import com.hfing.ticketflowapi.payment.dto.stripe.CreateStripeCheckoutCommand;
import com.hfing.ticketflowapi.payment.dto.stripe.StripeCheckoutSession;
import com.hfing.ticketflowapi.payment.dto.stripe.StripeLineItem;
import com.hfing.ticketflowapi.payment.dto.internal.PaymentReservation;
import com.hfing.ticketflowapi.payment.dto.internal.PaymentSessionReference;
import com.hfing.ticketflowapi.payment.dto.response.PaymentResponse;
import com.hfing.ticketflowapi.payment.entity.Payment;
import com.hfing.ticketflowapi.payment.enums.PaymentProvider;
import com.hfing.ticketflowapi.payment.enums.PaymentStatus;
import com.hfing.ticketflowapi.payment.mapper.PaymentMapper;
import com.hfing.ticketflowapi.payment.repository.PaymentRepository;
import com.hfing.ticketflowapi.payment.service.PaymentTransactionService;
import com.hfing.ticketflowapi.payment.service.StripeCheckoutService;
import com.hfing.ticketflowapi.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {
    @Mock private PaymentTransactionService paymentTransactionService;
    @Mock private StripeCheckoutService stripeCheckoutService;
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private BookingMapper bookingMapper;
    @Mock private PaymentMapper paymentMapper;
    @InjectMocks private BookingServiceImpl bookingService;

    @Test
    void checkoutCreatesGatewayPaymentOutsideReservationTransaction() {
        CheckoutRequest request = checkoutRequest();
        PaymentReservation reservation = newReservation(null);
        StripeCheckoutSession session = newSession("cs_test_1", "pi_1");
        CheckoutResponse expected = new CheckoutResponse(
                "booking-1", "show-1", new BigDecimal("300000"),
                BookingStatus.PENDING_PAYMENT, List.of(), null,
                Instant.parse("2026-07-21T03:15:00Z"), List.of());

        when(paymentTransactionService.reserve(
                org.mockito.ArgumentMatchers.eq("customer-1"),
                org.mockito.ArgumentMatchers.eq(request),
                org.mockito.ArgumentMatchers.eq("checkout-key-1"),
                any())).thenReturn(reservation);
        when(stripeCheckoutService.createSession(any(CreateStripeCheckoutCommand.class))).thenReturn(session);
        when(paymentTransactionService.attachGatewayPayment("payment-1", session)).thenReturn(expected);

        CheckoutResponse result = bookingService.checkout("customer-1", request, "checkout-key-1");

        assertThat(result).isSameAs(expected);
        verify(stripeCheckoutService).createSession(any(CreateStripeCheckoutCommand.class));
    }

    @Test
    void checkoutRetryRetrievesExistingProviderPaymentInsteadOfCreatingAnother() {
        CheckoutRequest request = checkoutRequest();
        PaymentReservation reservation = newReservation("cs_existing");
        StripeCheckoutSession session = newSession("cs_existing", "pi_existing");

        when(paymentTransactionService.reserve(any(), any(), any(), any())).thenReturn(reservation);
        when(stripeCheckoutService.retrieveSession("cs_existing")).thenReturn(session);

        bookingService.checkout("customer-1", request, "checkout-key-1");

        verify(stripeCheckoutService).retrieveSession("cs_existing");
        verify(stripeCheckoutService, never()).createSession(any());
    }

    @Test
    void concurrentCheckoutRecoversFromDatabaseIdempotencyConstraint() {
        CheckoutRequest request = checkoutRequest();
        PaymentReservation reservation = newReservation("cs_existing");
        StripeCheckoutSession session = newSession("cs_existing", "pi_existing");
        when(paymentTransactionService.reserve(any(), any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("idempotency conflict"));
        when(paymentTransactionService.findExistingReservation(any(), any(), any()))
                .thenReturn(Optional.of(reservation));
        when(stripeCheckoutService.retrieveSession("cs_existing")).thenReturn(session);

        bookingService.checkout("customer-1", request, "checkout-key-1");

        verify(paymentTransactionService).findExistingReservation(
                org.mockito.ArgumentMatchers.eq("customer-1"),
                org.mockito.ArgumentMatchers.eq("checkout-key-1"),
                any());
        verify(stripeCheckoutService, never()).createSession(any());
    }

    @Test
    void checkoutRejectsMissingIdempotencyKeyBeforeReservingInventory() {
        assertThatThrownBy(() -> bookingService.checkout("customer-1", checkoutRequest(), " "))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);

        verify(paymentTransactionService, never()).reserve(any(), any(), any(), any());
    }

    @Test
    void getMyBookingsReturnsRepositoryProjection() {
        BookingSummaryResponse summary = new BookingSummaryResponse(
                "booking-1", "Concert", "show-1", null,
                new BigDecimal("300000"), BookingStatus.CONFIRMED,
                Instant.parse("2026-07-21T03:00:00Z"));
        when(bookingRepository.findSummariesByCustomerId("customer-1")).thenReturn(List.of(summary));

        assertThat(bookingService.getMyBookings("customer-1")).containsExactly(summary);
    }

    @Test
    void getMyBookingDetailRejectsBookingOwnedByAnotherCustomer() {
        when(bookingRepository.findByIdAndCustomerId("booking-2", "customer-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getMyBookingDetail("customer-1", "booking-2"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOKING_NOT_FOUND);

        verify(paymentRepository, never()).findByBookingId(any());
    }

    @Test
    void getMyBookingDetailReturnsMappedPayment() {
        User customer = User.builder().id("customer-1").build();
        Event event = Event.builder().id("event-1").status(EventStatus.PUBLISHED).build();
        EventShow eventShow = EventShow.builder().id("show-1").event(event).build();
        Booking booking = Booking.builder()
                .id("booking-1")
                .customer(customer)
                .eventShow(eventShow)
                .status(BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("300000"))
                .build();
        PaymentResponse payment = PaymentResponse.builder()
                .id("payment-1")
                .amount(new BigDecimal("300000"))
                .currency("VND")
                .provider(PaymentProvider.STRIPE)
                .status(PaymentStatus.PAID)
                .providerSessionId("cs_test_1")
                .providerPaymentId("pi_1")
                .paidAt(Instant.parse("2026-07-21T03:05:00Z"))
                .build();
        when(bookingRepository.findByIdAndCustomerId("booking-1", "customer-1"))
                .thenReturn(Optional.of(booking));
        Payment paymentEntity = Payment.builder().id("payment-1").booking(booking).build();
        when(paymentRepository.findByBookingId("booking-1")).thenReturn(Optional.of(paymentEntity));
        when(paymentMapper.toResponse(paymentEntity)).thenReturn(payment);

        bookingService.getMyBookingDetail("customer-1", "booking-1");

        verify(bookingMapper).toBookingDetailResponse(booking, payment);
    }

    @Test
    void cancelBookingCancelsProviderBeforeReleasingLocalInventory() {
        PaymentSessionReference candidate = PaymentSessionReference.builder()
                .paymentId("payment-1")
                .providerSessionId("cs_test_1")
                .build();
        when(paymentTransactionService.getCancellationCandidate("customer-1", "booking-1"))
                .thenReturn(candidate);
        bookingService.cancelBooking("customer-1", "booking-1");

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(stripeCheckoutService, paymentTransactionService);
        inOrder.verify(stripeCheckoutService).expireSession("cs_test_1");
        inOrder.verify(paymentTransactionService).cancel("payment-1");
    }

    private CheckoutRequest checkoutRequest() {
        return new CheckoutRequest(
                "show-1", List.of(new CheckoutItemRequest("vip-1", 2)));
    }

    private PaymentReservation newReservation(String providerSessionId) {
        return PaymentReservation.builder()
                .paymentId("payment-1")
                .bookingId("booking-1")
                .amount(new BigDecimal("300000"))
                .currency("VND")
                .providerSessionId(providerSessionId)
                .providerPaymentId(providerSessionId == null ? null : "pi_existing")
                .customerEmail("customer@example.com")
                .expiresAt(Instant.parse("2026-07-21T03:31:00Z"))
                .items(List.of(StripeLineItem.builder()
                        .name("VIP")
                        .unitAmount(new BigDecimal("150000"))
                        .quantity(2)
                        .build()))
                .build();
    }

    private StripeCheckoutSession newSession(String sessionId, String paymentId) {
        return StripeCheckoutSession.builder()
                .providerSessionId(sessionId)
                .providerPaymentId(paymentId)
                .checkoutUrl("https://checkout.stripe.com/c/pay/" + sessionId)
                .build();
    }
}
