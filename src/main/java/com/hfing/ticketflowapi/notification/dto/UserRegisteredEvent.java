package com.hfing.ticketflowapi.notification.dto;


public record UserRegisteredEvent(
        String email,
        String firstName,
        String lastName
) {}