package com.hfing.ticketflowapi.booking.service.impl;

import com.hfing.ticketflowapi.booking.dto.request.CheckoutItemRequest;
import com.hfing.ticketflowapi.booking.dto.request.CheckoutRequest;
import com.hfing.ticketflowapi.booking.dto.response.BookingDetailResponse;
import com.hfing.ticketflowapi.booking.dto.response.BookingSummaryResponse;
import com.hfing.ticketflowapi.booking.dto.response.CheckoutResponse;
import com.hfing.ticketflowapi.booking.entity.Booking;
import com.hfing.ticketflowapi.booking.entity.BookingItem;
import com.hfing.ticketflowapi.booking.entity.Payment;
import com.hfing.ticketflowapi.booking.entity.Ticket;
import com.hfing.ticketflowapi.booking.enums.BookingStatus;
import com.hfing.ticketflowapi.booking.enums.PaymentMethod;
import com.hfing.ticketflowapi.booking.enums.PaymentStatus;
import com.hfing.ticketflowapi.booking.enums.TicketStatus;
import com.hfing.ticketflowapi.booking.mapper.BookingMapper;
import com.hfing.ticketflowapi.booking.repository.BookingRepository;
import com.hfing.ticketflowapi.booking.repository.PaymentRepository;
import com.hfing.ticketflowapi.booking.service.BookingService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final UserRepository userRepository;
    private final EventShowRepository eventShowRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional
    public CheckoutResponse checkout(String customerId, CheckoutRequest request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        EventShow eventShow = eventShowRepository.findById(request.eventShowId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_FOUND));

        if (eventShow.getEvent().getStatus() != EventStatus.PUBLISHED) {
            throw new AppException(ErrorCode.EVENT_NOT_PUBLISHED);
        }

        LocalDateTime paidAt = LocalDateTime.now();
        if (!eventShow.isOnSale(paidAt)) {
            throw new AppException(ErrorCode.SHOW_NOT_ON_SALE);
        }

        Set<String> ticketTypeIds = new HashSet<>();
        for (CheckoutItemRequest item : request.items()) {
            if (!ticketTypeIds.add(item.ticketTypeId())) {
                throw new AppException(ErrorCode.DUPLICATE_TICKET_TYPE);
            }
        }


        List<TicketType> lockedTicketTypes = ticketTypeRepository.findAllByIdInOrderByIdAsc(ticketTypeIds);
        if (lockedTicketTypes.size() != ticketTypeIds.size()) {
            throw new AppException(ErrorCode.TICKET_TYPE_NOT_FOUND);
        }

        Map<String, TicketType> ticketTypesById = new HashMap<>();
        lockedTicketTypes.forEach(ticketType -> ticketTypesById.put(ticketType.getId(), ticketType));

        Booking booking = Booking.builder()
                .customer(customer)
                .eventShow(eventShow)
                .totalAmount(BigDecimal.ZERO)
                .status(BookingStatus.PAID)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CheckoutItemRequest requestedItem : request.items()) {
            TicketType ticketType = ticketTypesById.get(requestedItem.ticketTypeId());
            validateTicketType(eventShow, ticketType, requestedItem.quantity());

            BigDecimal subtotal = ticketType.getPrice().multiply(BigDecimal.valueOf(requestedItem.quantity()));
            BookingItem bookingItem = BookingItem.builder()
                    .booking(booking)
                    .ticketType(ticketType)
                    .quantity(requestedItem.quantity())
                    .unitPrice(ticketType.getPrice())
                    .subtotal(subtotal)
                    .build();
            booking.getItems().add(bookingItem);

            ticketType.setSoldQuantity(ticketType.getSoldQuantity() + requestedItem.quantity());
            if (ticketType.getAvailableQuantity() == 0) {
                ticketType.setStatus(TicketTypeStatus.SOLD_OUT);
            }

            for (int i = 0; i < requestedItem.quantity(); i++) {
                booking.getTickets().add(Ticket.builder()
                        .booking(booking)
                        .ticketType(ticketType)
                        .ticketCode("TKT-" + UUID.randomUUID().toString().replace("-", "").toUpperCase())
                        .status(TicketStatus.VALID)
                        .build());
            }
            totalAmount = totalAmount.add(subtotal);
        }
        booking.setTotalAmount(totalAmount);

        Booking savedBooking = bookingRepository.save(booking);
        Payment payment = paymentRepository.save(Payment.builder()
                .booking(savedBooking)
                .amount(totalAmount)
                .method(PaymentMethod.FAKE)
                .status(PaymentStatus.SUCCESS)
                .transactionCode("FAKE-" + UUID.randomUUID().toString().replace("-", "").toUpperCase())
                .paidAt(paidAt)
                .build());

        return bookingMapper.toCheckoutResponse(savedBooking, payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingSummaryResponse> getMyBookings(String customerId) {
        return bookingRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(bookingMapper::toBookingSummaryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDetailResponse getMyBookingDetail(String customerId, String bookingId) {
        Booking booking = bookingRepository.findByIdAndCustomerId(bookingId, customerId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        return bookingMapper.toBookingDetailResponse(booking, payment);
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

}
