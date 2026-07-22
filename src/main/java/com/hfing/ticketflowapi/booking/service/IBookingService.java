package com.hfing.ticketflowapi.booking.service;

import com.hfing.ticketflowapi.booking.dto.request.CheckoutRequest;
import com.hfing.ticketflowapi.booking.dto.response.CheckoutResponse;
import com.hfing.ticketflowapi.booking.dto.response.BookingDetailResponse;
import com.hfing.ticketflowapi.booking.dto.response.BookingSummaryResponse;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IBookingService {
    CheckoutResponse checkout(String customerId, CheckoutRequest request, String clientIp);

    boolean completeVnPayPayment(String bookingId, long amount);

    void failVnPayPayment(String bookingId);

    void expirePendingBooking(String bookingId, Instant now);

    Page<BookingSummaryResponse> getMyBookings(String customerId, Pageable pageable);

    BookingDetailResponse getMyBookingDetail(String customerId, String bookingId);

    void cancelBooking(String customerId, String bookingId);
}
