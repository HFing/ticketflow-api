package com.hfing.ticketflowapi.auth.dto;

import lombok.Builder;


@Builder
public record TokenDetails(
        String value,
        String jwtId,
        long ttlSeconds
) {}