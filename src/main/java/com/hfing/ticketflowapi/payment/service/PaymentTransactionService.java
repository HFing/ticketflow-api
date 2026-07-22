package com.hfing.ticketflowapi.payment.service;

import com.hfing.ticketflowapi.booking.dto.request.CheckoutItemRequest;
import com.hfing.ticketflowapi.booking.dto.request.CheckoutRequest;
import com.hfing.ticketflowapi.payment.dto.response.CheckoutPaymentResponse;
import com.hfing.ticketflowapi.payment.dto.response.PaymentResponse;
import com.hfing.ticketflowapi.booking.dto.response.CheckoutResponse;
import com.hfing.ticketflowapi.booking.entity.Booking;
import com.hfing.ticketflowapi.booking.entity.BookingItem;
import com.hfing.ticketflowapi.payment.entity.Payment;
import com.hfing.ticketflowapi.booking.entity.Ticket;
import com.hfing.ticketflowapi.booking.enums.BookingStatus;
import com.hfing.ticketflowapi.payment.enums.PaymentProvider;
import com.hfing.ticketflowapi.payment.enums.PaymentStatus;
import com.hfing.ticketflowapi.booking.enums.TicketStatus;
import com.hfing.ticketflowapi.payment.config.PaymentProperties;
import com.hfing.ticketflowapi.payment.dto.stripe.StripeCheckoutSession;
import com.hfing.ticketflowapi.payment.dto.stripe.StripeWebhookEvent;
import com.hfing.ticketflowapi.payment.dto.internal.PaymentReservation;
import com.hfing.ticketflowapi.payment.dto.internal.PaymentSessionReference;
import com.hfing.ticketflowapi.payment.mapper.PaymentMapper;
import com.hfing.ticketflowapi.booking.repository.BookingRepository;
import com.hfing.ticketflowapi.payment.repository.PaymentRepository;
import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.event.entity.EventShow;
import com.hfing.ticketflowapi.event.entity.TicketType;
import com.hfing.ticketflowapi.event.enums.EventStatus;
import com.hfing.ticketflowapi.event.enums.TicketTypeStatus;
import com.hfing.ticketflowapi.event.repository.EventShowRepository;
import com.hfing.ticketflowapi.event.repository.TicketTypeRepository;
import com.hfing.ticketflowapi.user.entity.User;
import com.hfing.ticketflowapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PaymentTransactionService {
    private final UserRepository userRepository;
    private final EventShowRepository eventShowRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentProperties paymentProperties;
    private final Clock paymentClock;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public PaymentReservation reserve(
            String customerId,
            CheckoutRequest request,
            String idempotencyKey,
            String requestFingerprint) {
        Optional<Booking> existing = bookingRepository
                .findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey);
        if (existing.isPresent()) {
            return existingReservation(existing.get(), requestFingerprint);
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        EventShow eventShow = eventShowRepository.findById(request.eventShowId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_FOUND));

        validateEventShowOrThrow(eventShow);
        Set<String> ticketTypeIds = uniqueTicketTypeIds(request.items());
        List<TicketType> lockedTicketTypes = ticketTypeRepository.findAllByIdInOrderByIdAsc(ticketTypeIds);
        if (lockedTicketTypes.size() != ticketTypeIds.size()) {
            throw new AppException(ErrorCode.TICKET_TYPE_NOT_FOUND);
        }

        existing = bookingRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey);
        if (existing.isPresent()) {
            return existingReservation(existing.get(), requestFingerprint);
        }

        Map<String, TicketType> ticketTypesById = new HashMap<>();
        lockedTicketTypes.forEach(ticketType -> ticketTypesById.put(ticketType.getId(), ticketType));

        Instant now = paymentClock.instant();
        Booking booking = Booking.builder()
                .customer(customer)
                .eventShow(eventShow)
                .totalAmount(BigDecimal.ZERO)
                .status(BookingStatus.PENDING_PAYMENT)
                .idempotencyKey(idempotencyKey)
                .requestFingerprint(requestFingerprint)
                .expiresAt(now.plus(paymentProperties.holdDuration()))
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CheckoutItemRequest requestedItem : request.items()) {
            TicketType ticketType = ticketTypesById.get(requestedItem.ticketTypeId());
            validateTicketTypeOrThrow(eventShow, ticketType, requestedItem.quantity());

            BigDecimal subtotal = ticketType.getPrice()
                    .multiply(BigDecimal.valueOf(requestedItem.quantity()));
            booking.getItems().add(BookingItem.builder()
                    .booking(booking)
                    .ticketType(ticketType)
                    .quantity(requestedItem.quantity())
                    .unitPrice(ticketType.getPrice())
                    .subtotal(subtotal)
                    .build());

            ticketType.setHeldQuantity(ticketType.getHeldQuantity() + requestedItem.quantity());
            if (ticketType.getAvailableQuantity() == 0) {
                ticketType.setStatus(TicketTypeStatus.SOLD_OUT);
            }
            totalAmount = totalAmount.add(subtotal);
        }
        requireWholeVndAmount(totalAmount);
        booking.setTotalAmount(totalAmount);

        Booking savedBooking = bookingRepository.save(booking);
        Payment payment = paymentRepository.save(Payment.builder()
                .booking(savedBooking)
                .amount(totalAmount)
                .currency(paymentProperties.currency().toUpperCase(Locale.ROOT))
                .provider(PaymentProvider.STRIPE)
                .status(PaymentStatus.PENDING)
                .build());

        return toReservation(payment);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentReservation> findExistingReservation(
            String customerId,
            String idempotencyKey,
            String requestFingerprint) {
        return bookingRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey)
                .map(booking -> existingReservation(booking, requestFingerprint));
    }

    @Transactional
    public CheckoutResponse attachGatewayPayment(
            String paymentId,
            StripeCheckoutSession session) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getBooking().getStatus() != BookingStatus.PENDING_PAYMENT
                || payment.getStatus() == PaymentStatus.CANCELLED
                || payment.getStatus() == PaymentStatus.EXPIRED
                || payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new AppException(ErrorCode.PAYMENT_INVALID_STATE);
        }

        if (payment.getProviderPaymentId() == null) {
            payment.setProviderPaymentId(session.providerPaymentId());
        } else if (session.providerPaymentId() != null
                && !payment.getProviderPaymentId().equals(session.providerPaymentId())) {
            throw new AppException(ErrorCode.PAYMENT_INVALID_STATE);
        }
        if (payment.getProviderSessionId() == null) {
            payment.setProviderSessionId(session.providerSessionId());
        } else if (!payment.getProviderSessionId().equals(session.providerSessionId())) {
            throw new AppException(ErrorCode.PAYMENT_INVALID_STATE);
        }
        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.PROCESSING);
        }

        return toCheckoutResponse(payment, session.checkoutUrl());
    }

    @Transactional
    public void applyStripeEvent(StripeWebhookEvent event) {
        applyStripeEvent(findPaymentForUpdate(event.providerSessionId()), event);
    }

    private Payment findPaymentForUpdate(String providerSessionId) {
        return paymentRepository.findByProviderSessionIdForUpdate(providerSessionId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    private void applyStripeEvent(Payment payment, StripeWebhookEvent event) {

        if (payment.getProviderPaymentId() == null && event.providerPaymentId() != null) {
            payment.setProviderPaymentId(event.providerPaymentId());
        } else if (event.providerPaymentId() != null
                && !event.providerPaymentId().equals(payment.getProviderPaymentId())) {
            throw new AppException(ErrorCode.PAYMENT_DETAILS_MISMATCH);
        }

        validateStripeEvent(payment, event);
        PaymentStatus current = payment.getStatus();
        if (current == event.status() || isStaleTerminalEvent(current, event.status())) {
            return;
        }

        switch (event.status()) {
            case PROCESSING -> transitionToProcessing(payment);
            case FAILED -> transitionToFailed(payment);
            case CANCELLED -> cancelPaymentAndReleaseInventory(payment);
            case EXPIRED -> expirePaymentAndReleaseInventory(payment);
            case PAID -> confirmPaymentAndIssueTickets(payment, event.occurredAt());
            default -> throw new AppException(ErrorCode.PAYMENT_INVALID_STATE);
        }
    }

    @Transactional(readOnly = true)
    public Optional<PaymentSessionReference> getExpirationCandidate(String paymentId, Instant now) {
        return paymentRepository.findById(paymentId)
                .filter(payment -> payment.getBooking().getStatus() == BookingStatus.PENDING_PAYMENT)
                .filter(payment -> !payment.getBooking().getExpiresAt().isAfter(now))
                .map(paymentMapper::toSessionReference);
    }

    @Transactional(readOnly = true)
    public PaymentSessionReference getCancellationCandidate(String customerId, String bookingId) {
        Payment payment = paymentRepository
                .findByBookingIdAndBookingCustomerId(bookingId, customerId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        if (payment.getBooking().getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new AppException(ErrorCode.PAYMENT_INVALID_STATE);
        }
        return paymentMapper.toSessionReference(payment);
    }

    @Transactional
    public void cancel(String paymentId) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        Booking booking = payment.getBooking();
        if (booking.getStatus() == BookingStatus.CANCELLED
                && payment.getStatus() == PaymentStatus.CANCELLED) {
            return;
        }
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT
                || payment.getStatus() == PaymentStatus.PAID
                || payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new AppException(ErrorCode.PAYMENT_INVALID_STATE);
        }

        releaseHeldInventory(booking);
        booking.setStatus(BookingStatus.CANCELLED);
        payment.setStatus(PaymentStatus.CANCELLED);
    }

    @Transactional
    public void expire(String paymentId, Instant now) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        Booking booking = payment.getBooking();
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT
                || booking.getExpiresAt().isAfter(now)) {
            return;
        }

        releaseHeldInventory(booking);
        booking.setStatus(BookingStatus.EXPIRED);
        if (payment.getStatus() != PaymentStatus.PAID
                && payment.getStatus() != PaymentStatus.REFUNDED) {
            payment.setStatus(PaymentStatus.EXPIRED);
        }
    }

    private PaymentReservation existingReservation(Booking booking, String requestFingerprint) {
        if (!booking.getRequestFingerprint().equals(requestFingerprint)) {
            throw new AppException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
        }
        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        return toReservation(payment);
    }

    private PaymentReservation toReservation(Payment payment) {
        return paymentMapper.toReservation(payment);
    }

    private CheckoutResponse toCheckoutResponse(Payment payment, String checkoutUrl) {
        Booking booking = payment.getBooking();
        CheckoutPaymentResponse checkoutPayment =
                paymentMapper.toCheckoutPaymentResponse(payment, checkoutUrl);
        return paymentMapper.toCheckoutResponse(booking, checkoutPayment);
    }

    private void validateEventShowOrThrow(EventShow eventShow) {
        if (eventShow.getEvent().getStatus() != EventStatus.PUBLISHED) {
            throw new AppException(ErrorCode.EVENT_NOT_PUBLISHED);
        }
        if (!eventShow.isOnSale(LocalDateTime.now(paymentClock))) {
            throw new AppException(ErrorCode.SHOW_NOT_ON_SALE);
        }
    }

    private Set<String> uniqueTicketTypeIds(List<CheckoutItemRequest> items) {
        Set<String> ids = new HashSet<>();
        for (CheckoutItemRequest item : items) {
            if (!ids.add(item.ticketTypeId())) {
                throw new AppException(ErrorCode.DUPLICATE_TICKET_TYPE);
            }
        }
        return ids;
    }

    private void validateTicketTypeOrThrow(EventShow eventShow, TicketType ticketType, int quantity) {
        if (quantity <= 0) {
            throw new AppException(ErrorCode.TICKET_QUANTITY_INVALID);
        }
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

    private void requireWholeVndAmount(BigDecimal amount) {
        if (amount.stripTrailingZeros().scale() > 0) {
            throw new AppException(ErrorCode.PAYMENT_DETAILS_MISMATCH);
        }
    }

    private void validateStripeEvent(Payment payment, StripeWebhookEvent event) {
        if (!payment.getBooking().getId().equals(event.bookingId())
                || payment.getAmount().compareTo(BigDecimal.valueOf(event.amount())) != 0
                || !payment.getCurrency().equalsIgnoreCase(event.currency())) {
            throw new AppException(ErrorCode.PAYMENT_DETAILS_MISMATCH);
        }
    }

    private boolean isStaleTerminalEvent(PaymentStatus current, PaymentStatus incoming) {
        return current == PaymentStatus.REFUNDED
                || (current == PaymentStatus.PAID && incoming != PaymentStatus.REFUNDED)
                || current == PaymentStatus.CANCELLED
                || current == PaymentStatus.EXPIRED;
    }

    private void transitionToProcessing(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PENDING
                && payment.getStatus() != PaymentStatus.FAILED) {
            throw new AppException(ErrorCode.PAYMENT_INVALID_STATE);
        }
        payment.setStatus(PaymentStatus.PROCESSING);
    }

    private void transitionToFailed(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PENDING
                && payment.getStatus() != PaymentStatus.PROCESSING) {
            throw new AppException(ErrorCode.PAYMENT_INVALID_STATE);
        }
        payment.setStatus(PaymentStatus.FAILED);
    }

    private void cancelPaymentAndReleaseInventory(Payment payment) {
        if (payment.getStatus() == PaymentStatus.PAID
                || payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new AppException(ErrorCode.PAYMENT_INVALID_STATE);
        }
        Booking booking = payment.getBooking();
        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            releaseHeldInventory(booking);
            booking.setStatus(BookingStatus.CANCELLED);
        }
        payment.setStatus(PaymentStatus.CANCELLED);
    }

    private void expirePaymentAndReleaseInventory(Payment payment) {
        if (payment.getStatus() == PaymentStatus.PAID
                || payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new AppException(ErrorCode.PAYMENT_INVALID_STATE);
        }
        Booking booking = payment.getBooking();
        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            releaseHeldInventory(booking);
            booking.setStatus(BookingStatus.EXPIRED);
        }
        payment.setStatus(PaymentStatus.EXPIRED);
    }

    private void confirmPaymentAndIssueTickets(Payment payment, Instant paidAt) {
        if (payment.getStatus() != PaymentStatus.PENDING
                && payment.getStatus() != PaymentStatus.PROCESSING
                && payment.getStatus() != PaymentStatus.FAILED) {
            throw new AppException(ErrorCode.PAYMENT_INVALID_STATE);
        }
        Booking booking = payment.getBooking();
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new AppException(ErrorCode.PAYMENT_INVALID_STATE);
        }

        Map<String, TicketType> lockedTicketTypes = lockBookingTicketTypes(booking);
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
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(paidAt);
        applicationEventPublisher.publishEvent(paymentMapper.toPaymentCompletedEvent(payment));
    }

    private Map<String, TicketType> lockBookingTicketTypes(Booking booking) {
        Set<String> ids = new HashSet<>();
        booking.getItems().forEach(item -> ids.add(item.getTicketType().getId()));
        List<TicketType> locked = ticketTypeRepository.findAllByIdInOrderByIdAsc(ids);
        if (locked.size() != ids.size()) {
            throw new AppException(ErrorCode.TICKET_TYPE_NOT_FOUND);
        }
        Map<String, TicketType> byId = new HashMap<>();
        locked.forEach(ticketType -> byId.put(ticketType.getId(), ticketType));
        return byId;
    }

    private void releaseHeldInventory(Booking booking) {
        Map<String, TicketType> lockedTicketTypes = lockBookingTicketTypes(booking);
        for (BookingItem item : booking.getItems()) {
            TicketType ticketType = lockedTicketTypes.get(item.getTicketType().getId());
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
}
