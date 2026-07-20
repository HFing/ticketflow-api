package com.hfing.ticketflowapi.booking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CheckoutRequest(
        @NotBlank(message = "Event show id is required")
        String eventShowId,

        @NotEmpty(message = "At least one checkout item is required")
        @Size(max = 20, message = "Checkout cannot contain more than 20 ticket types")
        List<@Valid CheckoutItemRequest> items
) {
}
