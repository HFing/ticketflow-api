package com.hfing.ticketflowapi.event.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;


public record EventShowRequest(
        @NotNull(message = "Show start time is required")
        LocalDateTime startTime,

        @NotNull(message = "Show end time is required")
        LocalDateTime endTime
) {}