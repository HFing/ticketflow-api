package com.hfing.ticketflowapi.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INTERNAL_ERROR(500, "Unexpected error occurred while processing request in backend service", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_ALREADY_EXISTS(400, "User already exists", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(404, "User not found", HttpStatus.NOT_FOUND),
    ROLE_NOT_FOUND(404, "Role not found", HttpStatus.NOT_FOUND),
    TOKEN_GENERATION_FAILED(500, "Failed to generate JWT token", HttpStatus.INTERNAL_SERVER_ERROR),
    TOKEN_EXPIRED(401, "JWT token has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(401, "Invalid JWT token", HttpStatus.UNAUTHORIZED),

    MISSING_LOGOUT_INFO(400, "Authorization header or refresh token is missing", HttpStatus.BAD_REQUEST),

    UNAUTHORIZED(401, "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "Forbidden", HttpStatus.FORBIDDEN),

    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
