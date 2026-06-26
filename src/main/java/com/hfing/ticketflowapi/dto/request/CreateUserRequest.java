package com.hfing.ticketflowapi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;
public record CreateUserRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email is invalid")
        String email,

        @NotBlank(message = "Password is required")
        @Length(min = 8, message = "Password must be at least 8 characters long")
        String password,

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
        String phone
) {
}
