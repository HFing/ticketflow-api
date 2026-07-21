package com.hfing.ticketflowapi.booking.service;

import com.hfing.ticketflowapi.booking.dto.request.CheckoutItemRequest;
import com.hfing.ticketflowapi.booking.dto.request.CheckoutRequest;
import com.hfing.ticketflowapi.booking.dto.response.BookingSummaryResponse;
import com.hfing.ticketflowapi.booking.dto.response.PaymentResponse;
import com.hfing.ticketflowapi.booking.entity.Booking;
import com.hfing.ticketflowapi.booking.entity.Payment;
import com.hfing.ticketflowapi.booking.enums.BookingStatus;
import com.hfing.ticketflowapi.booking.enums.PaymentStatus;
import com.hfing.ticketflowapi.booking.enums.PaymentMethod;
import com.hfing.ticketflowapi.booking.entity.BookingItem;
import com.hfing.ticketflowapi.booking.repository.BookingRepository;
import com.hfing.ticketflowapi.booking.repository.PaymentRepository;
import com.hfing.ticketflowapi.booking.mapper.BookingMapper;
import com.hfing.ticketflowapi.booking.service.impl.BookingServiceImpl;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    @Mock private UserRepository userRepository;
    @Mock private EventShowRepository eventShowRepository;
    @Mock private TicketTypeRepository ticketTypeRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentRepository paymentRepository;
    @Spy private BookingMapper bookingMapper = Mappers.getMapper(BookingMapper.class);
    @InjectMocks private BookingServiceImpl bookingService;

    private User customer;
    private EventShow eventShow;
    private TicketType vip;

    @BeforeEach
    void setUp() {
        customer = User.builder().id("customer-1").email("customer@test.com").build();
        Event event = Event.builder()
                .id("event-1")
                .name("Summer Concert")
                .location("TMA Hall")
                .status(EventStatus.PUBLISHED)
                .build();
        LocalDateTime now = LocalDateTime.now();
        eventShow = EventShow.builder()
                .id("show-1")
                .event(event)
                .status(EventShowStatus.ON_SALE)
                .startTime(now.plusDays(1))
                .endTime(now.plusDays(1).plusHours(3))
                .saleStartTime(now.minusHours(1))
                .saleEndTime(now.plusHours(1))
                .build();
        vip = TicketType.builder()
                .id("vip-1")
                .name("VIP")
                .price(new BigDecimal("150.00"))
                .totalQuantity(10)
                .soldQuantity(3)
                .heldQuantity(1)
                .maxPerOrder(5)
                .status(TicketTypeStatus.ACTIVE)
                .eventShow(eventShow)
                .build();
    }

    @Test
    void checkout_whenValid_createsPaidBookingPaymentAndIndividualTickets() {
        CheckoutRequest request = new CheckoutRequest(
                "show-1", List.of(new CheckoutItemRequest("vip-1", 2)));
        when(userRepository.findById("customer-1")).thenReturn(Optional.of(customer));
        when(eventShowRepository.findById("show-1")).thenReturn(Optional.of(eventShow));
        when(ticketTypeRepository.findAllByIdInOrderByIdAsc(any())).thenReturn(List.of(vip));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = bookingService.checkout("customer-1", request);

        assertThat(response.status()).isEqualTo(BookingStatus.PAID);
        assertThat(response.totalAmount()).isEqualByComparingTo("300.00");
        assertThat(response.items()).hasSize(1);
        assertThat(response.tickets()).hasSize(2);
        assertThat(response.tickets()).allMatch(ticket -> ticket.ticketCode().startsWith("TKT-"));
        assertThat(response.payment().status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.payment().transactionCode()).startsWith("FAKE-");
        assertThat(vip.getSoldQuantity()).isEqualTo(5);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        assertThat(bookingCaptor.getValue().getItems()).hasSize(1);
        assertThat(bookingCaptor.getValue().getTickets()).hasSize(2);
    }

    @Test
    void checkout_whenInventoryIsInsufficient_doesNotCreateBooking() {
        vip.setSoldQuantity(9);
        vip.setHeldQuantity(0);
        CheckoutRequest request = new CheckoutRequest(
                "show-1", List.of(new CheckoutItemRequest("vip-1", 2)));
        when(userRepository.findById("customer-1")).thenReturn(Optional.of(customer));
        when(eventShowRepository.findById("show-1")).thenReturn(Optional.of(eventShow));
        when(ticketTypeRepository.findAllByIdInOrderByIdAsc(any())).thenReturn(List.of(vip));

        assertThatThrownBy(() -> bookingService.checkout("customer-1", request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_TICKET_QUANTITY);

        verify(bookingRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
        assertThat(vip.getSoldQuantity()).isEqualTo(9);
    }

    @Test
    void getMyBookings_returnsOnlyRepositoryResultsInSummaryForm() {
        BookingSummaryResponse summary = new BookingSummaryResponse(
                "booking-1",
                "Summer Concert",
                "show-1",
                eventShow.getStartTime(),
                new BigDecimal("300.00"),
                BookingStatus.PAID,
                Instant.parse("2026-07-17T01:00:00Z")
        );
        when(bookingRepository.findSummariesByCustomerId("customer-1"))
                .thenReturn(List.of(summary));

        var result = bookingService.getMyBookings("customer-1");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo("booking-1");
        assertThat(result.getFirst().eventName()).isEqualTo("Summer Concert");
        assertThat(result.getFirst().eventShowId()).isEqualTo("show-1");
        assertThat(result.getFirst().createdAt()).isEqualTo(summary.createdAt());
    }

    @Test
    void getMyBookingDetail_whenOwnedByCustomer_returnsDetailsAndPayment() {
        Booking booking = Booking.builder()
                .id("booking-1")
                .customer(customer)
                .eventShow(eventShow)
                .totalAmount(new BigDecimal("300.00"))
                .status(BookingStatus.PAID)
                .build();
        booking.setCreatedAt(Instant.parse("2026-07-17T01:00:00Z"));
        booking.getItems().add(BookingItem.builder()
                .id("item-1")
                .booking(booking)
                .ticketType(vip)
                .quantity(2)
                .unitPrice(new BigDecimal("150.00"))
                .subtotal(new BigDecimal("300.00"))
                .build());
        PaymentResponse payment = new PaymentResponse(
                "payment-1",
                new BigDecimal("300.00"),
                PaymentMethod.FAKE,
                PaymentStatus.SUCCESS,
                "FAKE-1",
                LocalDateTime.now()
        );
        when(bookingRepository.findByIdAndCustomerId("booking-1", "customer-1"))
                .thenReturn(Optional.of(booking));
        when(paymentRepository.findResponseByBookingId("booking-1")).thenReturn(Optional.of(payment));

        var result = bookingService.getMyBookingDetail("customer-1", "booking-1");

        assertThat(result.eventName()).isEqualTo("Summer Concert");
        assertThat(result.location()).isEqualTo("TMA Hall");
        assertThat(result.items()).hasSize(1);
        assertThat(result.payment().id()).isEqualTo("payment-1");
    }

    @Test
    void getMyBookingDetail_whenBookingBelongsToAnotherCustomer_returnsNotFound() {
        when(bookingRepository.findByIdAndCustomerId("booking-2", "customer-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getMyBookingDetail("customer-1", "booking-2"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOKING_NOT_FOUND);

        verify(paymentRepository, never()).findResponseByBookingId(any());
    }
}
