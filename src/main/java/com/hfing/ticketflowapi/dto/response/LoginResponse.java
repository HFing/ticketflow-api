package com.hfing.ticketflowapi.dto.response;

import lombok.Builder;

import java.util.Set;

@Builder
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String role
) {
}