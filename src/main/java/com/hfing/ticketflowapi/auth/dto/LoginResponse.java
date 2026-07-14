package com.hfing.ticketflowapi.auth.dto;

import lombok.Builder;


@Builder
public record LoginResponse(
                String accessToken,
                String role) {
}