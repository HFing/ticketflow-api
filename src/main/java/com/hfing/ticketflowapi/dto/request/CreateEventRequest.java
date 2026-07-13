package com.hfing.ticketflowapi.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

import com.hfing.ticketflowapi.common.enums.EventCategory;

public record CreateEventRequest(
                @NotBlank(message = "Event name is required") @Size(max = 100, message = "Event name cannot exceed 100 characters") String name,

                String description,

                @NotNull(message = "Start time is required") @Future(message = "Start time must be in the future") LocalDateTime startTime,

                @NotNull(message = "End time is required") LocalDateTime endTime,

                String location,

                String bannerUrl,

                String shortImageUrl,

                @NotNull(message = "Event category is required") EventCategory category,

                @Size(min = 1, message = "At least one show must be specified") List<EventShowRequest> shows) {
}
