package com.hfing.ticketflowapi.booking.controller;

import com.hfing.ticketflowapi.booking.dto.request.CheckoutRequest;
import com.hfing.ticketflowapi.booking.dto.response.CheckoutResponse;
import com.hfing.ticketflowapi.booking.dto.response.BookingDetailResponse;
import com.hfing.ticketflowapi.booking.dto.response.BookingSummaryResponse;
import com.hfing.ticketflowapi.booking.service.IBookingService;
import com.hfing.ticketflowapi.common.response.ApiResponse;
import com.hfing.ticketflowapi.common.response.PageResponse;
import com.hfing.ticketflowapi.common.validation.ControllerInputValidator;
import com.hfing.ticketflowapi.payment.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customer/bookings")
public class CustomerBookingController {
    private final IBookingService bookingService;
    private final VNPayService vnPayService;

    @PostMapping("/checkout")
    public ApiResponse<CheckoutResponse> checkout(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CheckoutRequest request,
            HttpServletRequest httpRequest
    ) {
        String customerId = ControllerInputValidator.requireAuthenticatedSubject(jwt);
        CheckoutRequest validatedRequest = ControllerInputValidator.requireRequestBody(request);
        CheckoutResponse data = bookingService.checkout(
                customerId, validatedRequest, vnPayService.getClientIp(httpRequest));
        return ApiResponse.<CheckoutResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Payment initialized successfully")
                .data(data)
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<BookingSummaryResponse>> getMyBookings(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable) {
        String customerId = ControllerInputValidator.requireAuthenticatedSubject(jwt);
        PageResponse<BookingSummaryResponse> data =
                PageResponse.from(bookingService.getMyBookings(customerId, pageable));
        return ApiResponse.<PageResponse<BookingSummaryResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Bookings retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/{bookingId}")
    public ApiResponse<BookingDetailResponse> getMyBookingDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String bookingId
    ) {
        String customerId = ControllerInputValidator.requireAuthenticatedSubject(jwt);
        BookingDetailResponse data = bookingService.getMyBookingDetail(customerId, bookingId);
        return ApiResponse.<BookingDetailResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Booking retrieved successfully")
                .data(data)
                .build();
    }

    @PostMapping("/{bookingId}/cancel")
    public ApiResponse<Void> cancelBooking(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String bookingId
    ) {
        String customerId = ControllerInputValidator.requireAuthenticatedSubject(jwt);
        bookingService.cancelBooking(customerId, bookingId);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Booking cancelled successfully")
                .build();
    }
}
