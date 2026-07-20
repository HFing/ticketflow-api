package com.hfing.ticketflowapi.booking.service;

import com.hfing.ticketflowapi.booking.dto.request.CheckoutRequest;
import com.hfing.ticketflowapi.booking.dto.response.CheckoutResponse;
import com.hfing.ticketflowapi.booking.dto.response.BookingDetailResponse;
import com.hfing.ticketflowapi.booking.dto.response.BookingSummaryResponse;

import java.util.List;

public interface BookingService {
    CheckoutResponse checkout(String customerId, CheckoutRequest request);

    List<BookingSummaryResponse> getMyBookings(String customerId);

    BookingDetailResponse getMyBookingDetail(String customerId, String bookingId);
}
