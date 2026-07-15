package com.hfing.ticketflowapi.event.dto;

import com.hfing.ticketflowapi.event.entity.Event;
import com.hfing.ticketflowapi.event.enums.EventCategory;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;



public record CreateEventRequest(
                @NotBlank(message = "Event name is required") @Size(max = 100, message = "Event name cannot exceed 100 characters") String name,

                String description,

                String location,

                String bannerUrl,

                String shortImageUrl,

                @NotNull(message = "Event category is required") EventCategory category) {
}