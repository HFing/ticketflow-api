package com.hfing.ticketflowapi.payment.mapper;

import com.hfing.ticketflowapi.booking.dto.response.CheckoutResponse;
import com.hfing.ticketflowapi.booking.entity.Booking;
import com.hfing.ticketflowapi.booking.entity.BookingItem;
import com.hfing.ticketflowapi.payment.entity.Payment;
import com.hfing.ticketflowapi.booking.mapper.BookingMapper;
import com.hfing.ticketflowapi.payment.dto.stripe.StripeLineItem;
import com.hfing.ticketflowapi.payment.dto.internal.PaymentReservation;
import com.hfing.ticketflowapi.payment.dto.internal.PaymentSessionReference;
import com.hfing.ticketflowapi.payment.dto.response.CheckoutPaymentResponse;
import com.hfing.ticketflowapi.payment.dto.response.PaymentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = BookingMapper.class)
public interface PaymentMapper {

    PaymentResponse toResponse(Payment payment);

    @Mapping(target = "checkoutUrl", source = "checkoutUrl")
    CheckoutPaymentResponse toCheckoutPaymentResponse(Payment payment, String checkoutUrl);

    @Mapping(target = "bookingId", source = "booking.id")
    @Mapping(target = "eventShowId", source = "booking.eventShow.id")
    @Mapping(target = "totalAmount", source = "booking.totalAmount")
    @Mapping(target = "status", source = "booking.status")
    @Mapping(target = "items", source = "booking.items")
    @Mapping(target = "payment", source = "payment")
    @Mapping(target = "expiresAt", source = "booking.expiresAt")
    @Mapping(target = "tickets", source = "booking.tickets")
    CheckoutResponse toCheckoutResponse(Booking booking, CheckoutPaymentResponse payment);

    @Mapping(target = "paymentId", source = "id")
    @Mapping(target = "bookingId", source = "booking.id")
    @Mapping(target = "customerEmail", source = "booking.customer.email")
    @Mapping(target = "expiresAt", source = "booking.expiresAt")
    @Mapping(target = "items", source = "booking.items")
    PaymentReservation toReservation(Payment payment);

    @Mapping(target = "name", source = "ticketType.name")
    @Mapping(target = "unitAmount", source = "unitPrice")
    StripeLineItem toLineItem(BookingItem item);

    @Mapping(target = "paymentId", source = "id")
    PaymentSessionReference toSessionReference(Payment payment);
}
