package com.hfing.ticketflowapi.dto.response;

import lombok.Builder;

@Builder
public record LoginResult(
        String accessToken,
        String refreshToken,
        String role
) {
}
