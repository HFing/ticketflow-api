package com.hfing.ticketflowapi.booking.service;

import com.hfing.ticketflowapi.booking.dto.request.CheckoutRequest;
import com.hfing.ticketflowapi.booking.dto.response.CheckoutResponse;
import com.hfing.ticketflowapi.booking.dto.response.BookingDetailResponse;
import com.hfing.ticketflowapi.booking.dto.response.BookingSummaryResponse;

import java.util.List;
import java.time.Instant;

public interface IBookingService {
    CheckoutResponse checkout(String customerId, CheckoutRequest request, String clientIp);

    boolean completeVnPayPayment(String bookingId, long amount);

    void failVnPayPayment(String bookingId);

    void expirePendingBooking(String bookingId, Instant now);

    List<BookingSummaryResponse> getMyBookings(String customerId);

    BookingDetailResponse getMyBookingDetail(String customerId, String bookingId);

    void cancelBooking(String customerId, String bookingId);
}
