package com.hfing.ticketflowapi.event.dto;

import java.time.LocalDateTime;


public record EventShowResponse(
        String id,
        LocalDateTime startTime,
        LocalDateTime endTime
) {}