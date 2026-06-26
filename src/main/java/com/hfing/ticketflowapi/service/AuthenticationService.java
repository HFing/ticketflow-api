package com.hfing.ticketflowapi.service;

import com.hfing.ticketflowapi.dto.request.LoginRequest;
import com.hfing.ticketflowapi.dto.response.LoginResponse;
import com.hfing.ticketflowapi.dto.response.LoginResult;

public interface AuthenticationService {
    LoginResult login(LoginRequest request);
    LoginResponse refreshToken(String refreshToken );
}
