package com.hfing.ticketflowapi.booking.service;

import com.hfing.ticketflowapi.booking.entity.Booking;
import com.hfing.ticketflowapi.booking.entity.BookingItem;
import com.hfing.ticketflowapi.booking.enums.BookingStatus;
import com.hfing.ticketflowapi.booking.mapper.BookingMapper;
import com.hfing.ticketflowapi.booking.repository.BookingRepository;
import com.hfing.ticketflowapi.booking.service.impl.BookingServiceImpl;
import com.hfing.ticketflowapi.event.entity.Event;
import com.hfing.ticketflowapi.event.entity.EventShow;
import com.hfing.ticketflowapi.event.entity.TicketType;
import com.hfing.ticketflowapi.event.enums.TicketTypeStatus;
import com.hfing.ticketflowapi.event.repository.EventShowRepository;
import com.hfing.ticketflowapi.event.repository.TicketTypeRepository;
import com.hfing.ticketflowapi.notification.dto.PaymentCompletedEvent;
import com.hfing.ticketflowapi.payment.entity.Payment;
import com.hfing.ticketflowapi.payment.enums.PaymentStatus;
import com.hfing.ticketflowapi.payment.config.VNPayConfig;
import com.hfing.ticketflowapi.payment.repository.PaymentRepository;
import com.hfing.ticketflowapi.payment.service.VNPayService;
import com.hfing.ticketflowapi.user.entity.User;
import com.hfing.ticketflowapi.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {
    @Mock private UserRepository userRepository;
    @Mock private EventShowRepository eventShowRepository;
    @Mock private TicketTypeRepository ticketTypeRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private BookingMapper bookingMapper;
    @Mock private VNPayService vnPayService;
    @Mock private VNPayConfig vnPayConfig;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks private BookingServiceImpl bookingService;

    @Test
    void successfulVnPayPaymentPublishesEmailNotificationEventOnlyOnce() {
        User customer = User.builder()
                .id("customer-1")
                .email("customer@example.com")
                .firstName("Test")
                .lastName("User")
                .build();
        Event event = Event.builder()
                .name("Live Concert")
                .location("Ho Chi Minh City")
                .venue("TMA Hall")
                .build();
        EventShow eventShow = EventShow.builder()
                .id("show-1")
                .event(event)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .build();
        TicketType ticketType = TicketType.builder()
                .id("ticket-type-1")
                .name("VIP")
                .eventShow(eventShow)
                .totalQuantity(10)
                .soldQuantity(0)
                .heldQuantity(1)
                .maxPerOrder(5)
                .status(TicketTypeStatus.ACTIVE)
                .price(new BigDecimal("100000"))
                .build();
        Booking booking = Booking.builder()
                .id("booking-1")
                .customer(customer)
                .eventShow(eventShow)
                .totalAmount(new BigDecimal("100000"))
                .status(BookingStatus.PENDING_PAYMENT)
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
        booking.getItems().add(BookingItem.builder()
                .booking(booking)
                .ticketType(ticketType)
                .quantity(1)
                .unitPrice(new BigDecimal("100000"))
                .subtotal(new BigDecimal("100000"))
                .build());
        Payment payment = Payment.builder()
                .id("payment-1")
                .booking(booking)
                .amount(new BigDecimal("100000"))
                .currency("VND")
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByBookingId("booking-1")).thenReturn(Optional.of(payment));
        when(ticketTypeRepository.findAllByIdInOrderByIdAsc(Set.of("ticket-type-1")))
                .thenReturn(List.of(ticketType));

        bookingService.completeVnPayPayment("booking-1", 10_000_000L);
        bookingService.completeVnPayPayment("booking-1", 10_000_000L);

        ArgumentCaptor<PaymentCompletedEvent> eventCaptor =
                ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());
        PaymentCompletedEvent publishedEvent = eventCaptor.getValue();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getTickets()).hasSize(1);
        assertThat(publishedEvent.customerEmail()).isEqualTo("customer@example.com");
        assertThat(publishedEvent.bookingId()).isEqualTo("booking-1");
        assertThat(publishedEvent.tickets()).hasSize(1);
        assertThat(publishedEvent.tickets().getFirst().ticketTypeName()).isEqualTo("VIP");
    }

    @Test
    void failedVnPayPaymentReleasesHeldTickets() {
        PaymentFixture fixture = paymentFixture(Instant.now().plusSeconds(300));
        stubLockedPayment(fixture);

        bookingService.failVnPayPayment("booking-1");
        bookingService.failVnPayPayment("booking-1");

        assertThat(fixture.ticketType().getHeldQuantity()).isZero();
        assertThat(fixture.ticketType().getStatus()).isEqualTo(TicketTypeStatus.ACTIVE);
        assertThat(fixture.booking().getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void expiredBookingReleasesHeldTickets() {
        Instant now = Instant.now();
        PaymentFixture fixture = paymentFixture(now.minusSeconds(1));
        stubLockedPayment(fixture);

        bookingService.expirePendingBooking("booking-1", now);
        bookingService.expirePendingBooking("booking-1", now);

        assertThat(fixture.ticketType().getHeldQuantity()).isZero();
        assertThat(fixture.ticketType().getStatus()).isEqualTo(TicketTypeStatus.ACTIVE);
        assertThat(fixture.booking().getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void successfulCallbackAfterExpirationDoesNotConfirmPayment() {
        PaymentFixture fixture = paymentFixture(Instant.now().minusSeconds(1));
        stubLockedPayment(fixture);

        boolean completed = bookingService.completeVnPayPayment("booking-1", 10_000_000L);

        assertThat(completed).isFalse();
        assertThat(fixture.ticketType().getHeldQuantity()).isZero();
        assertThat(fixture.ticketType().getSoldQuantity()).isZero();
        assertThat(fixture.booking().getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    private PaymentFixture paymentFixture(Instant expiresAt) {
        User customer = User.builder().id("customer-1").build();
        EventShow eventShow = EventShow.builder()
                .id("show-1")
                .event(Event.builder().name("Concert").build())
                .build();
        TicketType ticketType = TicketType.builder()
                .id("ticket-type-1")
                .name("VIP")
                .eventShow(eventShow)
                .totalQuantity(1)
                .soldQuantity(0)
                .heldQuantity(1)
                .maxPerOrder(1)
                .status(TicketTypeStatus.SOLD_OUT)
                .price(new BigDecimal("100000"))
                .build();
        Booking booking = Booking.builder()
                .id("booking-1")
                .customer(customer)
                .eventShow(eventShow)
                .totalAmount(new BigDecimal("100000"))
                .status(BookingStatus.PENDING_PAYMENT)
                .expiresAt(expiresAt)
                .build();
        booking.getItems().add(BookingItem.builder()
                .booking(booking)
                .ticketType(ticketType)
                .quantity(1)
                .unitPrice(new BigDecimal("100000"))
                .subtotal(new BigDecimal("100000"))
                .build());
        Payment payment = Payment.builder()
                .id("payment-1")
                .booking(booking)
                .amount(new BigDecimal("100000"))
                .currency("VND")
                .status(PaymentStatus.PENDING)
                .build();
        return new PaymentFixture(payment, booking, ticketType);
    }

    private void stubLockedPayment(PaymentFixture fixture) {
        when(paymentRepository.findByBookingId("booking-1")).thenReturn(Optional.of(fixture.payment()));
        when(ticketTypeRepository.findAllByIdInOrderByIdAsc(Set.of("ticket-type-1")))
                .thenReturn(List.of(fixture.ticketType()));
    }

    private record PaymentFixture(Payment payment, Booking booking, TicketType ticketType) {
    }
}
