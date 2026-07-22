package com.hfing.ticketflowapi.common.exception;

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
    REQUEST_BODY_REQUIRED(400, "Request body is required", HttpStatus.BAD_REQUEST),

    MISSING_LOGOUT_INFO(400, "Authorization header or refresh token is missing", HttpStatus.BAD_REQUEST),

    UNAUTHORIZED(401, "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "Forbidden", HttpStatus.FORBIDDEN),

    EVENT_NOT_FOUND(404, "Event not found", HttpStatus.NOT_FOUND),
    EVENT_CREATION_PAST_START(400, "Event start time cannot be in the past", HttpStatus.BAD_REQUEST),
    EVENT_START_AFTER_END(400, "Event start time must be before end time", HttpStatus.BAD_REQUEST),
    EVENT_NOT_DRAFT(400, "Event is not in DRAFT status", HttpStatus.BAD_REQUEST),
    EVENT_PUBLISH_MISSING_INFO(400, "Cannot publish event: missing required information (description, location, or price)", HttpStatus.BAD_REQUEST),
    EVENT_FORBIDDEN_MODIFICATION(403, "You do not have permission to modify this event", HttpStatus.FORBIDDEN),
    EVENT_NOT_DRAFT_OR_REJECTED(400, "Event must be in DRAFT or REJECTED status to submit for review", HttpStatus.BAD_REQUEST),
    EVENT_NOT_PENDING_REVIEW(400, "Event must be in PENDING_REVIEW status", HttpStatus.BAD_REQUEST),
    EVENT_NO_SHOWS(400, "Event must have at least one show", HttpStatus.BAD_REQUEST),
    SHOW_NO_TICKET_TYPES(400, "Each show must have at least one ticket type", HttpStatus.BAD_REQUEST),
    SHOW_INVALID_TIME(400, "Show start time must be before end time", HttpStatus.BAD_REQUEST),
    SHOW_INVALID_SALE_START_TIME(400, "Ticket sale start time must be before show start time", HttpStatus.BAD_REQUEST),
    SHOW_INVALID_SALE_END_TIME(400, "Ticket sale end time must be before or equal to show start time", HttpStatus.BAD_REQUEST),
    TICKET_INVALID_PRICE(400, "Ticket price must be greater than 0", HttpStatus.BAD_REQUEST),
    TICKET_INVALID_QUANTITY(400, "Ticket total quantity must be greater than 0", HttpStatus.BAD_REQUEST),
    TICKET_INVALID_MAX_PER_ORDER(400, "Ticket max per order must be greater than 0 and less than or equal to total quantity", HttpStatus.BAD_REQUEST),
    SHOW_NOT_FOUND(404, "Event show not found", HttpStatus.NOT_FOUND),

    EVENT_NOT_PUBLISHED(400, "Event is not published", HttpStatus.BAD_REQUEST),
    SHOW_NOT_ON_SALE(400, "Event show is not currently on sale", HttpStatus.BAD_REQUEST),
    TICKET_TYPE_NOT_FOUND(404, "Ticket type not found", HttpStatus.NOT_FOUND),
    TICKET_TYPE_NOT_IN_SHOW(400, "Ticket type does not belong to the selected event show", HttpStatus.BAD_REQUEST),
    TICKET_TYPE_NOT_AVAILABLE(400, "Ticket type is not available", HttpStatus.BAD_REQUEST),
    TICKET_QUANTITY_EXCEEDED(400, "Requested quantity exceeds the maximum allowed per order", HttpStatus.BAD_REQUEST),
    TICKET_QUANTITY_INVALID(400, "Ticket quantity must be greater than 0", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_TICKET_QUANTITY(409, "Not enough tickets available", HttpStatus.CONFLICT),
    DUPLICATE_TICKET_TYPE(400, "A ticket type must appear only once in checkout items", HttpStatus.BAD_REQUEST),
    BOOKING_NOT_FOUND(404, "Booking not found", HttpStatus.NOT_FOUND),
    TICKET_NOT_FOUND(404, "Ticket not found", HttpStatus.NOT_FOUND),
    TICKET_FORBIDDEN_ACCESS(403, "Ticket does not belong to your event", HttpStatus.FORBIDDEN),
    TICKET_NOT_VALID(400, "Ticket is not valid for check-in", HttpStatus.BAD_REQUEST),
    TICKET_ALREADY_USED(409, "Ticket has already been used", HttpStatus.CONFLICT),
    SHOW_CANCELLED(400, "Event show has been cancelled", HttpStatus.BAD_REQUEST),
    IDEMPOTENCY_KEY_REQUIRED(400, "Idempotency-Key header is required", HttpStatus.BAD_REQUEST),
    IDEMPOTENCY_KEY_REUSED(409, "Idempotency-Key was already used for a different checkout", HttpStatus.CONFLICT),
    PAYMENT_NOT_FOUND(404, "Payment not found", HttpStatus.NOT_FOUND),
    PAYMENT_GATEWAY_NOT_AVAILABLE(400, "Selected payment gateway is not available", HttpStatus.BAD_REQUEST),
    PAYMENT_INITIALIZATION_FAILED(502, "Unable to initialize payment", HttpStatus.BAD_GATEWAY),
    PAYMENT_WEBHOOK_INVALID(400, "Invalid payment webhook", HttpStatus.BAD_REQUEST),
    PAYMENT_DETAILS_MISMATCH(409, "Payment amount or currency does not match booking", HttpStatus.CONFLICT),
    PAYMENT_INVALID_STATE(409, "Payment state transition is not allowed", HttpStatus.CONFLICT),
    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
