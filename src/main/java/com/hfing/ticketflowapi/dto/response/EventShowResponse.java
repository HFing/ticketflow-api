package com.hfing.ticketflowapi.dto.response;

import java.time.LocalDateTime;

public record EventShowResponse(
        String id,
        LocalDateTime startTime,
        LocalDateTime endTime
) {}
