package com.hfing.ticketflowapi.dto.response;

import lombok.Builder;

@Builder
public record LoginResponse(
                String accessToken,
                String role) {
}