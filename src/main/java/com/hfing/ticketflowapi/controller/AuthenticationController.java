package com.hfing.ticketflowapi.controller;

import com.hfing.ticketflowapi.dto.request.LoginRequest;
import com.hfing.ticketflowapi.dto.response.ApiResponse;
import com.hfing.ticketflowapi.dto.response.LoginResponse;
import com.hfing.ticketflowapi.service.AuthenticationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    private final AuthenticationService authService;
    @PostMapping("/login")
    ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse loginResponse = authService.login(request);

        return ApiResponse.<LoginResponse>builder()
                .code(HttpServletResponse.SC_OK)
                .message("Login successful")
                .data(loginResponse)
                .build();
    }

}
