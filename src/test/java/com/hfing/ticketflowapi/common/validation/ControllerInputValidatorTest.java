package com.hfing.ticketflowapi.common.validation;

import com.hfing.ticketflowapi.booking.dto.request.CheckoutRequest;
import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ControllerInputValidatorTest {

    @Test
    void requireAuthenticatedSubjectReturnsCustomerId() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("customer-1")
                .build();

        assertEquals("customer-1", ControllerInputValidator.requireAuthenticatedSubject(jwt));
    }

    @Test
    void requireAuthenticatedSubjectRejectsMissingJwt() {
        AppException exception = assertThrows(
                AppException.class,
                () -> ControllerInputValidator.requireAuthenticatedSubject(null));

        assertEquals(ErrorCode.TOKEN_INVALID, exception.getErrorCode());
    }

    @Test
    void requireAuthenticatedSubjectRejectsBlankCustomerId() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("   ")
                .build();

        AppException exception = assertThrows(
                AppException.class,
                () -> ControllerInputValidator.requireAuthenticatedSubject(jwt));

        assertEquals(ErrorCode.TOKEN_INVALID, exception.getErrorCode());
    }

    @Test
    void requireRequestBodyRejectsNullAndReturnsNonNullRequest() {
        AppException exception = assertThrows(
                AppException.class,
                () -> ControllerInputValidator.requireRequestBody(null));
        assertEquals(ErrorCode.REQUEST_BODY_REQUIRED, exception.getErrorCode());

        CheckoutRequest request = new CheckoutRequest("show-1", List.of());
        assertSame(request, ControllerInputValidator.requireRequestBody(request));
    }
}
