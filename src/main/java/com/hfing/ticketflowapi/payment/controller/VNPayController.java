package com.hfing.ticketflowapi.payment.controller;

import com.hfing.ticketflowapi.booking.service.IBookingService;
import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.common.response.ApiResponse;
import com.hfing.ticketflowapi.payment.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class VNPayController {
    private final VNPayService vnPayService;
    private final IBookingService bookingService;

    @GetMapping({"/api/v1/payments/vnpay-callback", "/api/v1/vnpay/payment-return"})
    public ApiResponse<Map<String, String>> paymentReturn(HttpServletRequest request) {
        if (!vnPayService.hasValidSignature(request)) {
            throw new AppException(ErrorCode.PAYMENT_WEBHOOK_INVALID);
        }

        String bookingId = request.getParameter("vnp_TxnRef");
        if (bookingId == null || bookingId.isBlank()) {
            throw new AppException(ErrorCode.PAYMENT_DETAILS_MISMATCH);
        }
        boolean providerSuccessful = "00".equals(request.getParameter("vnp_ResponseCode"))
                && "00".equals(request.getParameter("vnp_TransactionStatus"));
        boolean successful = false;
        if (providerSuccessful) {
            try {
                successful = bookingService.completeVnPayPayment(
                        bookingId, Long.parseLong(request.getParameter("vnp_Amount")));
            } catch (NumberFormatException exception) {
                throw new AppException(ErrorCode.PAYMENT_DETAILS_MISMATCH);
            }
        } else {
            bookingService.failVnPayPayment(bookingId);
        }

        return ApiResponse.<Map<String, String>>builder()
                .code(HttpStatus.OK.value())
                .message(successful ? "Payment completed successfully" : "Payment was not completed")
                .data(Map.of(
                        "bookingId", bookingId,
                        "status", successful ? "SUCCESS" : "FAILED",
                        "transactionId", request.getParameter("vnp_TransactionNo") == null
                                ? "" : request.getParameter("vnp_TransactionNo")))
                .build();
    }
}
