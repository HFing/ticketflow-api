package com.hfing.ticketflowapi.auth.dto;

import lombok.Builder;


@Builder
public record LoginResult(
        String accessToken,
        String refreshToken,
        String role
) {
}