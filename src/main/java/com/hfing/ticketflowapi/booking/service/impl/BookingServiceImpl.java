package com.hfing.ticketflowapi.booking.service.impl;

import com.hfing.ticketflowapi.booking.dto.request.CheckoutItemRequest;
import com.hfing.ticketflowapi.booking.dto.request.CheckoutRequest;
import com.hfing.ticketflowapi.booking.dto.response.BookingDetailResponse;
import com.hfing.ticketflowapi.booking.dto.response.BookingSummaryResponse;
import com.hfing.ticketflowapi.booking.dto.response.CheckoutResponse;
import com.hfing.ticketflowapi.booking.entity.Booking;
import com.hfing.ticketflowapi.booking.entity.BookingItem;
import com.hfing.ticketflowapi.booking.entity.Ticket;
import com.hfing.ticketflowapi.booking.enums.BookingStatus;
import com.hfing.ticketflowapi.booking.enums.TicketStatus;
import com.hfing.ticketflowapi.booking.mapper.BookingMapper;
import com.hfing.ticketflowapi.booking.repository.BookingRepository;
import com.hfing.ticketflowapi.booking.service.IBookingService;
import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.event.entity.EventShow;
import com.hfing.ticketflowapi.event.entity.TicketType;
import com.hfing.ticketflowapi.event.enums.EventStatus;
import com.hfing.ticketflowapi.event.enums.TicketTypeStatus;
import com.hfing.ticketflowapi.event.repository.EventShowRepository;
import com.hfing.ticketflowapi.event.repository.TicketTypeRepository;
import com.hfing.ticketflowapi.notification.dto.PaidTicketInfo;
import com.hfing.ticketflowapi.notification.dto.PaymentCompletedEvent;
import com.hfing.ticketflowapi.payment.entity.Payment;
import com.hfing.ticketflowapi.payment.enums.PaymentStatus;
import com.hfing.ticketflowapi.payment.config.VNPayConfig;
import com.hfing.ticketflowapi.payment.repository.PaymentRepository;
import com.hfing.ticketflowapi.payment.service.VNPayService;
import com.hfing.ticketflowapi.user.entity.User;
import com.hfing.ticketflowapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements IBookingService {
    private final UserRepository userRepository;
    private final EventShowRepository eventShowRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final BookingMapper bookingMapper;
    private final VNPayService vnPayService;
    private final VNPayConfig vnPayConfig;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public CheckoutResponse checkout(String customerId, CheckoutRequest request, String clientIp) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        EventShow eventShow = eventShowRepository.findById(request.eventShowId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_FOUND));
        validateEventShow(eventShow);

        Set<String> requestedIds = new HashSet<>();
        for (CheckoutItemRequest item : request.items()) {
            if (!requestedIds.add(item.ticketTypeId())) {
                throw new AppException(ErrorCode.DUPLICATE_TICKET_TYPE);
            }
        }

        List<TicketType> ticketTypes = ticketTypeRepository.findAllByIdInOrderByIdAsc(requestedIds);
        if (ticketTypes.size() != requestedIds.size()) {
            throw new AppException(ErrorCode.TICKET_TYPE_NOT_FOUND);
        }
        Map<String, TicketType> ticketTypesById = new HashMap<>();
        ticketTypes.forEach(ticketType -> ticketTypesById.put(ticketType.getId(), ticketType));

        Booking booking = Booking.builder()
                .customer(customer)
                .eventShow(eventShow)
                .totalAmount(BigDecimal.ZERO)
                .status(BookingStatus.PENDING_PAYMENT)
                .expiresAt(Instant.now().plusSeconds(vnPayConfig.getExpireMinutes() * 60L))
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CheckoutItemRequest item : request.items()) {
            TicketType ticketType = ticketTypesById.get(item.ticketTypeId());
            validateTicketType(eventShow, ticketType, item.quantity());
            BigDecimal subtotal = ticketType.getPrice().multiply(BigDecimal.valueOf(item.quantity()));
            booking.getItems().add(BookingItem.builder()
                    .booking(booking)
                    .ticketType(ticketType)
                    .quantity(item.quantity())
                    .unitPrice(ticketType.getPrice())
                    .subtotal(subtotal)
                    .build());
            ticketType.setHeldQuantity(ticketType.getHeldQuantity() + item.quantity());
            if (ticketType.getAvailableQuantity() == 0) {
                ticketType.setStatus(TicketTypeStatus.SOLD_OUT);
            }
            totalAmount = totalAmount.add(subtotal);
        }
        if (totalAmount.stripTrailingZeros().scale() > 0) {
            throw new AppException(ErrorCode.PAYMENT_DETAILS_MISMATCH);
        }

        booking.setTotalAmount(totalAmount);
        Booking savedBooking = bookingRepository.save(booking);
        paymentRepository.save(Payment.builder()
                .booking(savedBooking)
                .amount(totalAmount)
                .currency("VND")
                .status(PaymentStatus.PENDING)
                .build());

        String paymentUrl = vnPayService.createPaymentUrl(totalAmount, savedBooking.getId(), clientIp);
        return bookingMapper.toCheckoutResponse(savedBooking, paymentUrl);
    }

    @Override
    @Transactional
    public boolean completeVnPayPayment(String bookingId, long vnPayAmount) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        Booking booking = payment.getBooking();
        if (payment.getStatus() == PaymentStatus.SUCCESS && booking.getStatus() == BookingStatus.CONFIRMED) {
            return true;
        }
        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT
                && !booking.getExpiresAt().isAfter(Instant.now())) {
            expirePaymentAndReleaseInventory(payment);
            return false;
        }
        if (payment.getStatus() != PaymentStatus.PENDING
                || booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new AppException(ErrorCode.PAYMENT_INVALID_STATE);
        }

        BigDecimal callbackAmount = BigDecimal.valueOf(vnPayAmount, 2);
        if (payment.getAmount().compareTo(callbackAmount) != 0
                || !"VND".equalsIgnoreCase(payment.getCurrency())) {
            throw new AppException(ErrorCode.PAYMENT_DETAILS_MISMATCH);
        }

        Map<String, TicketType> lockedTicketTypes = lockTicketTypes(booking);
        for (BookingItem item : booking.getItems()) {
            TicketType ticketType = lockedTicketTypes.get(item.getTicketType().getId());
            if (ticketType.getHeldQuantity() < item.getQuantity()) {
                throw new AppException(ErrorCode.PAYMENT_INVALID_STATE);
            }
            ticketType.setHeldQuantity(ticketType.getHeldQuantity() - item.getQuantity());
            ticketType.setSoldQuantity(ticketType.getSoldQuantity() + item.getQuantity());
            for (int index = 0; index < item.getQuantity(); index++) {
                booking.getTickets().add(Ticket.builder()
                        .booking(booking)
                        .ticketType(ticketType)
                        .ticketCode("TKT-" + UUID.randomUUID().toString().replace("-", "").toUpperCase())
                        .status(TicketStatus.VALID)
                        .build());
            }
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        payment.setStatus(PaymentStatus.SUCCESS);
        Instant paidAt = Instant.now();
        payment.setPaidAt(paidAt);
        applicationEventPublisher.publishEvent(toPaymentCompletedEvent(payment, paidAt));
        return true;
    }

    @Override
    @Transactional
    public void failVnPayPayment(String bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        Booking booking = payment.getBooking();
        if (payment.getStatus() == PaymentStatus.FAILED
                || payment.getStatus() == PaymentStatus.CANCELLED
                || payment.getStatus() == PaymentStatus.EXPIRED
                || payment.getStatus() == PaymentStatus.SUCCESS
                || booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return;
        }

        releaseHeldTickets(booking);
        booking.setStatus(BookingStatus.CANCELLED);
        payment.setStatus(PaymentStatus.FAILED);
    }

    @Override
    @Transactional
    public void expirePendingBooking(String bookingId, Instant now) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        Booking booking = payment.getBooking();
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT
                || booking.getExpiresAt().isAfter(now)) {
            return;
        }

        expirePaymentAndReleaseInventory(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingSummaryResponse> getMyBookings(String customerId, Pageable pageable) {
        return bookingRepository.findSummariesByCustomerId(customerId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDetailResponse getMyBookingDetail(String customerId, String bookingId) {
        Booking booking = bookingRepository.findByIdAndCustomerId(bookingId, customerId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        return bookingMapper.toBookingDetailResponse(booking);
    }

    @Override
    @Transactional
    public void cancelBooking(String customerId, String bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        Booking booking = payment.getBooking();
        if (!booking.getCustomer().getId().equals(customerId)) {
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return;
        }
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new AppException(ErrorCode.PAYMENT_INVALID_STATE);
        }

        releaseHeldTickets(booking);
        booking.setStatus(BookingStatus.CANCELLED);
        payment.setStatus(PaymentStatus.CANCELLED);
    }

    private void expirePaymentAndReleaseInventory(Payment payment) {
        Booking booking = payment.getBooking();
        releaseHeldTickets(booking);
        booking.setStatus(BookingStatus.EXPIRED);
        payment.setStatus(PaymentStatus.EXPIRED);
    }

    private void validateEventShow(EventShow eventShow) {
        if (eventShow.getEvent().getStatus() != EventStatus.PUBLISHED) {
            throw new AppException(ErrorCode.EVENT_NOT_PUBLISHED);
        }
        if (!eventShow.isOnSale(LocalDateTime.now())) {
            throw new AppException(ErrorCode.SHOW_NOT_ON_SALE);
        }
    }

    private void validateTicketType(EventShow eventShow, TicketType ticketType, int quantity) {
        if (!ticketType.getEventShow().getId().equals(eventShow.getId())) {
            throw new AppException(ErrorCode.TICKET_TYPE_NOT_IN_SHOW);
        }
        if (ticketType.getStatus() != TicketTypeStatus.ACTIVE) {
            throw new AppException(ErrorCode.TICKET_TYPE_NOT_AVAILABLE);
        }
        if (quantity > ticketType.getMaxPerOrder()) {
            throw new AppException(ErrorCode.TICKET_QUANTITY_EXCEEDED);
        }
        if (quantity > ticketType.getAvailableQuantity()) {
            throw new AppException(ErrorCode.INSUFFICIENT_TICKET_QUANTITY);
        }
    }

    private Map<String, TicketType> lockTicketTypes(Booking booking) {
        Set<String> ids = new HashSet<>();
        booking.getItems().forEach(item -> ids.add(item.getTicketType().getId()));
        List<TicketType> ticketTypes = ticketTypeRepository.findAllByIdInOrderByIdAsc(ids);
        if (ticketTypes.size() != ids.size()) {
            throw new AppException(ErrorCode.TICKET_TYPE_NOT_FOUND);
        }
        Map<String, TicketType> result = new HashMap<>();
        ticketTypes.forEach(ticketType -> result.put(ticketType.getId(), ticketType));
        return result;
    }

    private void releaseHeldTickets(Booking booking) {
        Map<String, TicketType> ticketTypes = lockTicketTypes(booking);
        for (BookingItem item : booking.getItems()) {
            TicketType ticketType = ticketTypes.get(item.getTicketType().getId());
            if (ticketType.getHeldQuantity() < item.getQuantity()) {
                throw new AppException(ErrorCode.PAYMENT_INVALID_STATE);
            }
            ticketType.setHeldQuantity(ticketType.getHeldQuantity() - item.getQuantity());
            if (ticketType.getStatus() == TicketTypeStatus.SOLD_OUT
                    && ticketType.getAvailableQuantity() > 0) {
                ticketType.setStatus(TicketTypeStatus.ACTIVE);
            }
        }
    }

    private PaymentCompletedEvent toPaymentCompletedEvent(Payment payment, Instant paidAt) {
        Booking booking = payment.getBooking();
        EventShow eventShow = booking.getEventShow();
        User customer = booking.getCustomer();
        List<PaidTicketInfo> tickets = booking.getTickets().stream()
                .map(ticket -> new PaidTicketInfo(
                        ticket.getTicketCode(), ticket.getTicketType().getName()))
                .toList();

        return new PaymentCompletedEvent(
                payment.getId(),
                booking.getId(),
                customer.getEmail(),
                customer.getFirstName(),
                customer.getLastName(),
                eventShow.getEvent().getName(),
                eventShow.getEvent().getLocation(),
                eventShow.getEvent().getVenue(),
                eventShow.getStartTime(),
                eventShow.getEndTime(),
                payment.getAmount(),
                payment.getCurrency(),
                paidAt,
                tickets);
    }
}
