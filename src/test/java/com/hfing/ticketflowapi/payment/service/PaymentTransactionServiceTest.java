package com.hfing.ticketflowapi.payment.service;

import com.hfing.ticketflowapi.booking.dto.request.CheckoutItemRequest;
import com.hfing.ticketflowapi.booking.dto.request.CheckoutRequest;
import com.hfing.ticketflowapi.booking.entity.Booking;
import com.hfing.ticketflowapi.booking.entity.BookingItem;
import com.hfing.ticketflowapi.payment.entity.Payment;
import com.hfing.ticketflowapi.booking.enums.BookingStatus;
import com.hfing.ticketflowapi.payment.enums.PaymentProvider;
import com.hfing.ticketflowapi.payment.enums.PaymentStatus;
import com.hfing.ticketflowapi.payment.config.PaymentProperties;
import com.hfing.ticketflowapi.payment.dto.stripe.StripeWebhookEvent;
import com.hfing.ticketflowapi.payment.dto.internal.PaymentReservation;
import com.hfing.ticketflowapi.payment.mapper.PaymentMapper;
import com.hfing.ticketflowapi.booking.repository.BookingRepository;
import com.hfing.ticketflowapi.payment.repository.PaymentRepository;
import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.event.entity.Event;
import com.hfing.ticketflowapi.event.entity.EventShow;
import com.hfing.ticketflowapi.event.entity.TicketType;
import com.hfing.ticketflowapi.event.enums.EventShowStatus;
import com.hfing.ticketflowapi.event.enums.EventStatus;
import com.hfing.ticketflowapi.event.enums.TicketTypeStatus;
import com.hfing.ticketflowapi.event.repository.EventShowRepository;
import com.hfing.ticketflowapi.event.repository.TicketTypeRepository;
import com.hfing.ticketflowapi.user.entity.User;
import com.hfing.ticketflowapi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentTransactionServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-21T03:00:00Z");

    @Mock private UserRepository userRepository;
    @Mock private EventShowRepository eventShowRepository;
    @Mock private TicketTypeRepository ticketTypeRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentRepository paymentRepository;

    private PaymentTransactionService service;
    private User customer;
    private EventShow eventShow;
    private TicketType vip;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneId.systemDefault());
        PaymentProperties properties = new PaymentProperties(
                "VND",
                Duration.ofMinutes(5),
                Duration.ofSeconds(30),
                100,
                new PaymentProperties.Stripe(
                        "unused", "unused", true,
                        "http://localhost/success", "http://localhost/cancel"));
        PaymentMapper mapper = Mappers.getMapper(PaymentMapper.class);
        service = new PaymentTransactionService(
                userRepository,
                eventShowRepository,
                ticketTypeRepository,
                bookingRepository,
                paymentRepository,
                mapper,
                properties,
                clock);

        customer = User.builder().id("customer-1").build();
        Event event = Event.builder()
                .id("event-1")
                .status(EventStatus.PUBLISHED)
                .build();
        LocalDateTime now = LocalDateTime.ofInstant(NOW, clock.getZone());
        eventShow = EventShow.builder()
                .id("show-1")
                .event(event)
                .status(EventShowStatus.ON_SALE)
                .startTime(now.plusDays(1))
                .endTime(now.plusDays(1).plusHours(2))
                .saleStartTime(now.minusHours(1))
                .saleEndTime(now.plusHours(1))
                .build();
        vip = TicketType.builder()
                .id("vip-1")
                .name("VIP")
                .price(new BigDecimal("150000"))
                .totalQuantity(10)
                .soldQuantity(3)
                .heldQuantity(1)
                .maxPerOrder(5)
                .status(TicketTypeStatus.ACTIVE)
                .eventShow(eventShow)
                .build();
    }

    @Test
    void reserveLocksInventoryAndCreatesPendingBookingAndPayment() {
        stubValidReservation();

        PaymentReservation result = service.reserve(
                "customer-1", checkoutRequest(), "checkout-key", "fingerprint");

        assertThat(result.paymentId()).isEqualTo("payment-1");
        assertThat(result.amount()).isEqualByComparingTo("300000");
        assertThat(result.currency()).isEqualTo("VND");
        assertThat(vip.getHeldQuantity()).isEqualTo(3);
        assertThat(vip.getSoldQuantity()).isEqualTo(3);

        org.mockito.ArgumentCaptor<Booking> bookingCaptor =
                org.mockito.ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        Booking booking = bookingCaptor.getValue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
        assertThat(booking.getTickets()).isEmpty();

        org.mockito.ArgumentCaptor<Payment> paymentCaptor =
                org.mockito.ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(paymentCaptor.getValue().getProvider()).isEqualTo(PaymentProvider.STRIPE);
    }

    @Test
    void reserveRejectsInsufficientInventoryWithoutCreatingBooking() {
        vip.setSoldQuantity(9);
        vip.setHeldQuantity(0);
        when(userRepository.findById("customer-1")).thenReturn(Optional.of(customer));
        when(eventShowRepository.findById("show-1")).thenReturn(Optional.of(eventShow));
        when(ticketTypeRepository.findAllByIdInOrderByIdAsc(any())).thenReturn(List.of(vip));

        assertThatThrownBy(() -> service.reserve(
                "customer-1", checkoutRequest(), "checkout-key", "fingerprint"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_TICKET_QUANTITY);

        verify(bookingRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void reserveRejectsIdempotencyKeyReusedWithDifferentPayload() {
        Booking existing = Booking.builder()
                .id("booking-1")
                .requestFingerprint("original-fingerprint")
                .build();
        when(bookingRepository.findByCustomerIdAndIdempotencyKey("customer-1", "checkout-key"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.reserve(
                "customer-1", checkoutRequest(), "checkout-key", "different-fingerprint"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REUSED);

        verify(ticketTypeRepository, never()).findAllByIdInOrderByIdAsc(any());
    }

    @Test
    void successfulWebhookConfirmsBookingMovesHeldToSoldAndIssuesTicketsOnce() {
        Payment payment = pendingPayment();
        when(paymentRepository.findByProviderSessionIdForUpdate("cs_test_1"))
                .thenReturn(Optional.of(payment));
        when(ticketTypeRepository.findAllByIdInOrderByIdAsc(any())).thenReturn(List.of(vip));
        StripeWebhookEvent event = new StripeWebhookEvent(
                "booking-1", "cs_test_1", "pi_1", PaymentStatus.PAID, 300000, "vnd", NOW);

        service.applyStripeEvent(event);
        service.applyStripeEvent(event);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isEqualTo(NOW);
        assertThat(payment.getBooking().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(vip.getHeldQuantity()).isEqualTo(1);
        assertThat(vip.getSoldQuantity()).isEqualTo(5);
        assertThat(payment.getBooking().getTickets()).hasSize(2);
        verify(ticketTypeRepository, times(1)).findAllByIdInOrderByIdAsc(any());
    }

    @Test
    void staleFailedWebhookCannotMoveSuccessfulPaymentBackward() {
        Payment payment = pendingPayment();
        payment.setStatus(PaymentStatus.PAID);
        payment.getBooking().setStatus(BookingStatus.CONFIRMED);
        when(paymentRepository.findByProviderSessionIdForUpdate("cs_test_1"))
                .thenReturn(Optional.of(payment));

        service.applyStripeEvent(new StripeWebhookEvent(
                "booking-1", "cs_test_1", "pi_1", PaymentStatus.FAILED, 300000, "vnd", NOW));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getBooking().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(ticketTypeRepository, never()).findAllByIdInOrderByIdAsc(any());
    }

    @Test
    void paidWebhookWithMismatchedBookingMetadataIsRejected() {
        Payment payment = pendingPayment();
        when(paymentRepository.findByProviderSessionIdForUpdate("cs_test_1"))
                .thenReturn(Optional.of(payment));
        StripeWebhookEvent event = new StripeWebhookEvent(
                "booking-2", "cs_test_1", "pi_1", PaymentStatus.PAID, 300000, "vnd", NOW);

        assertThatThrownBy(() -> service.applyStripeEvent(event))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_DETAILS_MISMATCH);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(payment.getBooking().getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        verify(ticketTypeRepository, never()).findAllByIdInOrderByIdAsc(any());
    }

    @Test
    void expireReleasesHeldInventoryAndMarksBookingAndPaymentExpired() {
        Payment payment = pendingPayment();
        payment.getBooking().setExpiresAt(NOW.minusSeconds(1));
        when(paymentRepository.findByIdForUpdate("payment-1")).thenReturn(Optional.of(payment));
        when(ticketTypeRepository.findAllByIdInOrderByIdAsc(any())).thenReturn(List.of(vip));

        service.expire("payment-1", NOW);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(payment.getBooking().getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(vip.getHeldQuantity()).isEqualTo(1);
        assertThat(vip.getSoldQuantity()).isEqualTo(3);
    }

    @Test
    void cancelReleasesHeldInventoryAndMarksBookingAndPaymentCancelled() {
        Payment payment = pendingPayment();
        when(paymentRepository.findByIdForUpdate("payment-1")).thenReturn(Optional.of(payment));
        when(ticketTypeRepository.findAllByIdInOrderByIdAsc(any())).thenReturn(List.of(vip));

        service.cancel("payment-1");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(payment.getBooking().getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(vip.getHeldQuantity()).isEqualTo(1);
        assertThat(vip.getSoldQuantity()).isEqualTo(3);
    }

    private void stubValidReservation() {
        when(userRepository.findById("customer-1")).thenReturn(Optional.of(customer));
        when(eventShowRepository.findById("show-1")).thenReturn(Optional.of(eventShow));
        when(ticketTypeRepository.findAllByIdInOrderByIdAsc(any())).thenReturn(List.of(vip));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId("booking-1");
            return booking;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId("payment-1");
            return payment;
        });
    }

    private Payment pendingPayment() {
        Booking booking = Booking.builder()
                .id("booking-1")
                .customer(customer)
                .eventShow(eventShow)
                .totalAmount(new BigDecimal("300000"))
                .status(BookingStatus.PENDING_PAYMENT)
                .expiresAt(NOW.plusSeconds(60))
                .build();
        booking.getItems().add(BookingItem.builder()
                .id("item-1")
                .booking(booking)
                .ticketType(vip)
                .quantity(2)
                .unitPrice(new BigDecimal("150000"))
                .subtotal(new BigDecimal("300000"))
                .build());
        vip.setHeldQuantity(3);
        return Payment.builder()
                .id("payment-1")
                .booking(booking)
                .amount(new BigDecimal("300000"))
                .currency("VND")
                .provider(PaymentProvider.STRIPE)
                .providerPaymentId("pi_1")
                .status(PaymentStatus.PROCESSING)
                .build();
    }

    private CheckoutRequest checkoutRequest() {
        return new CheckoutRequest(
                "show-1", List.of(new CheckoutItemRequest("vip-1", 2)));
    }
}
