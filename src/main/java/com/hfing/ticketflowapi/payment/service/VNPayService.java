package com.hfing.ticketflowapi.payment.service;

import com.hfing.ticketflowapi.payment.config.VNPayConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VNPayService {
    private static final DateTimeFormatter VNPAY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final VNPayConfig config;

    public String createPaymentUrl(BigDecimal amount, String bookingId, String clientIp) {
        long amountInVnd = amount.longValueExact();
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", config.getTmnCode());
        params.put("vnp_Amount", String.valueOf(Math.multiplyExact(amountInVnd, 100)));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", bookingId);
        params.put("vnp_OrderInfo", "Thanh toan booking " + bookingId);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", config.getReturnUrl());
        params.put("vnp_IpAddr", clientIp);
        params.put("vnp_CreateDate", now.format(VNPAY_DATE_FORMAT));
        params.put("vnp_ExpireDate", now.plusMinutes(config.getExpireMinutes()).format(VNPAY_DATE_FORMAT));

        String query = buildQuery(params);
        String secureHash = hmacSha512(config.getHashSecret(), query);
        return config.getPayUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    public boolean hasValidSignature(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            if (!"vnp_SecureHash".equals(name) && !"vnp_SecureHashType".equals(name)) {
                String value = request.getParameter(name);
                if (value != null && !value.isBlank()) {
                    fields.put(name, value);
                }
            }
        }

        String expectedHash = hmacSha512(config.getHashSecret(), buildQuery(fields));
        String receivedHash = request.getParameter("vnp_SecureHash");
        return receivedHash != null && expectedHash.equalsIgnoreCase(receivedHash);
    }

    public String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String buildQuery(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder result = new StringBuilder();
        for (String fieldName : fieldNames) {
            String value = params.get(fieldName);
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append('&');
            }
            result.append(encode(fieldName)).append('=').append(encode(value));
        }
        return result.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    private String hmacSha512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            hmac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] result = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(result.length * 2);
            for (byte value : result) {
                hex.append(String.format("%02x", value & 0xff));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot create VNPay secure hash", exception);
        }
    }
}
