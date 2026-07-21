package com.hfing.ticketflowapi.booking.mapper;

import com.hfing.ticketflowapi.booking.dto.response.BookingDetailResponse;
import com.hfing.ticketflowapi.booking.dto.response.BookingItemResponse;
import com.hfing.ticketflowapi.booking.dto.response.CheckoutResponse;
import com.hfing.ticketflowapi.booking.dto.response.PaymentResponse;
import com.hfing.ticketflowapi.booking.dto.response.TicketResponse;
import com.hfing.ticketflowapi.booking.dto.response.OrganizerTicketResponse;
import com.hfing.ticketflowapi.booking.entity.Booking;
import com.hfing.ticketflowapi.booking.entity.BookingItem;
import com.hfing.ticketflowapi.booking.entity.Payment;
import com.hfing.ticketflowapi.booking.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface BookingMapper {

    @Mapping(target = "eventName", source = "booking.eventShow.event.name")
    @Mapping(target = "location", source = "booking.eventShow.event.location")
    @Mapping(target = "showStartTime", source = "booking.eventShow.startTime")
    @Mapping(target = "showEndTime", source = "booking.eventShow.endTime")
    @Mapping(target = "id", source = "booking.id")
    @Mapping(target = "totalAmount", source = "booking.totalAmount")
    @Mapping(target = "status", source = "booking.status")
    @Mapping(target = "items", source = "booking.items")
    @Mapping(target = "createdAt", source = "booking.createdAt")
    @Mapping(target = "payment", source = "payment")
    BookingDetailResponse toBookingDetailResponse(Booking booking, PaymentResponse payment);

    @Mapping(target = "bookingId", source = "booking.id")
    @Mapping(target = "eventShowId", source = "booking.eventShow.id")
    @Mapping(target = "totalAmount", source = "booking.totalAmount")
    @Mapping(target = "status", source = "booking.status")
    @Mapping(target = "items", source = "booking.items")
    @Mapping(target = "tickets", source = "booking.tickets")
    @Mapping(target = "payment", source = "payment")
    CheckoutResponse toCheckoutResponse(Booking booking, Payment payment);

    @Mapping(target = "ticketTypeId", source = "ticketType.id")
    @Mapping(target = "ticketTypeName", source = "ticketType.name")
    BookingItemResponse toBookingItemResponse(BookingItem bookingItem);

    PaymentResponse toPaymentResponse(Payment payment);

    @Mapping(target = "ticketTypeId", source = "ticketType.id")
    @Mapping(target = "ticketTypeName", source = "ticketType.name")
    TicketResponse toTicketResponse(Ticket ticket);

    @Mapping(target = "ticketTypeId", source = "ticketType.id")
    @Mapping(target = "ticketTypeName", source = "ticketType.name")
    @Mapping(target = "eventShowId", source = "booking.eventShow.id")
    @Mapping(target = "customerId", source = "booking.customer.id")
    @Mapping(target = "customerEmail", source = "booking.customer.email")
    OrganizerTicketResponse toOrganizerTicketResponse(Ticket ticket);
}
