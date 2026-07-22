package com.hfing.ticketflowapi.booking.mapper;

import com.hfing.ticketflowapi.booking.dto.response.BookingDetailResponse;
import com.hfing.ticketflowapi.booking.dto.response.CheckoutResponse;
import com.hfing.ticketflowapi.booking.dto.response.BookingItemResponse;
import com.hfing.ticketflowapi.booking.dto.response.TicketResponse;
import com.hfing.ticketflowapi.booking.dto.response.OrganizerTicketResponse;
import com.hfing.ticketflowapi.booking.entity.Booking;
import com.hfing.ticketflowapi.booking.entity.BookingItem;
import com.hfing.ticketflowapi.booking.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BookingMapper {

    @Mapping(target = "eventName", source = "eventShow.event.name")
    @Mapping(target = "location", source = "eventShow.event.location")
    @Mapping(target = "showStartTime", source = "eventShow.startTime")
    @Mapping(target = "showEndTime", source = "eventShow.endTime")
    BookingDetailResponse toBookingDetailResponse(Booking booking);

    @Mapping(target = "bookingId", source = "booking.id")
    @Mapping(target = "eventShowId", source = "booking.eventShow.id")
    @Mapping(target = "paymentUrl", source = "paymentUrl")
    CheckoutResponse toCheckoutResponse(Booking booking, String paymentUrl);

    @Mapping(target = "ticketTypeId", source = "ticketType.id")
    @Mapping(target = "ticketTypeName", source = "ticketType.name")
    BookingItemResponse toBookingItemResponse(BookingItem bookingItem);

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
