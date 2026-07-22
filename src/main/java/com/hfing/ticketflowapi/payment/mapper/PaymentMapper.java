package com.hfing.ticketflowapi.payment.mapper;

import com.hfing.ticketflowapi.booking.dto.response.CheckoutResponse;
import com.hfing.ticketflowapi.booking.entity.Booking;
import com.hfing.ticketflowapi.booking.entity.BookingItem;
import com.hfing.ticketflowapi.booking.entity.Ticket;
import com.hfing.ticketflowapi.notification.dto.PaidTicketInfo;
import com.hfing.ticketflowapi.notification.dto.PaymentCompletedEvent;
import com.hfing.ticketflowapi.payment.entity.Payment;
import com.hfing.ticketflowapi.booking.mapper.BookingMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = BookingMapper.class)
public interface PaymentMapper {

}
