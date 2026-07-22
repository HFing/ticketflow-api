package com.hfing.ticketflowapi.payment.controller;

import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/stripe")
public class StripeRedirectController {

    @GetMapping("/success")
    public ApiResponse<String> paymentSuccess(
            @RequestParam(name = "session_id", required = false) String sessionId) {
        if (!StringUtils.hasText(sessionId) || !sessionId.startsWith("cs_")) {
            throw new AppException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        return ApiResponse.<String>builder()
                .code(HttpStatus.OK.value())
                .message("Checkout completed; payment confirmation is handled by webhook")
                .data(sessionId)
                .build();
    }
}
