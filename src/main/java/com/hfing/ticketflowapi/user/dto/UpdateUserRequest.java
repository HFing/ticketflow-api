package com.hfing.ticketflowapi.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;


public record UpdateUserRequest(
        @NotBlank(message = "First name is required")
        @Length(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Length(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Pattern(
                regexp = "^(\\+84|0)[0-9]{9}$",
                message = "Phone number is invalid"
        )
        String phone,

        String avatarKey,

        String coverKey,

        String description
) {}