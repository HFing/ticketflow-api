package com.hfing.ticketflowapi.dto;

import lombok.Builder;

@Builder
public record TokenDetails(
        String value,
        String jwtId,
        long ttlSeconds
) {}
