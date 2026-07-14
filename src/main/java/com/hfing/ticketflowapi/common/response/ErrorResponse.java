package com.hfing.ticketflowapi.common.response;

import lombok.Builder;


@Builder
public record ErrorResponse(
        int code,
        String message,
        String error,
        String path,
        long timestamp
) {
}