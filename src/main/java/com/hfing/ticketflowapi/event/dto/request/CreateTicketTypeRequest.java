package com.hfing.ticketflowapi.event.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateTicketTypeRequest(
        @NotBlank(message = "Ticket type name is required")
        String name,

        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        BigDecimal price,

        @NotNull(message = "Total quantity is required")
        @Min(value = 1, message = "Total quantity must be at least 1")
        Integer totalQuantity,

        @NotNull(message = "Max per order is required")
        @Min(value = 1, message = "Max per order must be at least 1")
        Integer maxPerOrder
) {}
