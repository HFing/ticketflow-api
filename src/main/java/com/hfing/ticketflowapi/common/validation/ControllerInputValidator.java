package com.hfing.ticketflowapi.common.validation;

import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

public final class ControllerInputValidator {
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 255;

    private ControllerInputValidator() {
    }

    public static String requireAuthenticatedSubject(Jwt jwt) {
        if (jwt == null || !StringUtils.hasText(jwt.getSubject())) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
        return jwt.getSubject();
    }

    public static <T> T requireRequestBody(T request) {
        if (request == null) {
            throw new AppException(ErrorCode.REQUEST_BODY_REQUIRED);
        }
        return request;
    }

    public static String requireIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)
                || idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new AppException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }
        return idempotencyKey;
    }
}
