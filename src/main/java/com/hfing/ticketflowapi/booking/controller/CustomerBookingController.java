package com.hfing.ticketflowapi.booking.controller;

import com.hfing.ticketflowapi.booking.dto.request.CheckoutRequest;
import com.hfing.ticketflowapi.booking.dto.response.CheckoutResponse;
import com.hfing.ticketflowapi.booking.dto.response.BookingDetailResponse;
import com.hfing.ticketflowapi.booking.dto.response.BookingSummaryResponse;
import com.hfing.ticketflowapi.booking.service.IBookingService;
import com.hfing.ticketflowapi.common.response.ApiResponse;
import com.hfing.ticketflowapi.common.validation.ControllerInputValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customer/bookings")
public class CustomerBookingController {
    private final IBookingService bookingService;

    @PostMapping("/checkout")
    public ApiResponse<CheckoutResponse> checkout(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CheckoutRequest request
    ) {
        String customerId = ControllerInputValidator.requireAuthenticatedSubject(jwt);
        CheckoutRequest validatedRequest = ControllerInputValidator.requireRequestBody(request);
        CheckoutResponse data = bookingService.checkout(customerId, validatedRequest);
        return ApiResponse.<CheckoutResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Checkout completed successfully")
                .data(data)
                .build();
    }

    @GetMapping
    public ApiResponse<List<BookingSummaryResponse>> getMyBookings(@AuthenticationPrincipal Jwt jwt) {
        String customerId = ControllerInputValidator.requireAuthenticatedSubject(jwt);
        List<BookingSummaryResponse> data = bookingService.getMyBookings(customerId);
        return ApiResponse.<List<BookingSummaryResponse>>builder()
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
}
