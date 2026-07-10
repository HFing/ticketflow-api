package com.hfing.ticketflowapi.dto.event;

public record UserRegisteredEvent(
        String email,
        String firstName,
        String lastName
) {}
