package com.hfing.ticketflowapi.payment.service;

import com.hfing.ticketflowapi.payment.config.VNPayConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class VNPayServiceTest {
    private VNPayService vnPayService;

    @BeforeEach
    void setUp() {
        VNPayConfig config = new VNPayConfig();
        config.setTmnCode("TEST_CODE");
        config.setHashSecret("test-secret");
        config.setPayUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        config.setReturnUrl("http://localhost:8080/api/v1/payments/vnpay-callback");
        config.setExpireMinutes(5);
        vnPayService = new VNPayService(config);
    }

    @Test
    void createsSignedPaymentUrlForBooking() {
        String url = vnPayService.createPaymentUrl(
                new BigDecimal("150000"), "booking-1", "127.0.0.1");

        assertThat(url)
                .contains("vnp_Amount=15000000")
                .contains("vnp_TxnRef=booking-1")
                .contains("vnp_TmnCode=TEST_CODE")
                .contains("vnp_SecureHash=");
    }

    @Test
    void validatesReturnSignatureAndRejectsTampering() {
        String url = vnPayService.createPaymentUrl(
                new BigDecimal("150000"), "booking-1", "127.0.0.1");
        MockHttpServletRequest request = requestFromQuery(URI.create(url).getRawQuery());

        assertThat(vnPayService.hasValidSignature(request)).isTrue();

        request.setParameter("vnp_Amount", "100");
        assertThat(vnPayService.hasValidSignature(request)).isFalse();
    }

    private MockHttpServletRequest requestFromQuery(String query) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            request.setParameter(
                    URLDecoder.decode(parts[0], StandardCharsets.US_ASCII),
                    URLDecoder.decode(parts[1], StandardCharsets.US_ASCII));
        }
        return request;
    }
}
