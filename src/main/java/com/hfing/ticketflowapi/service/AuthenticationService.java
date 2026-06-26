package com.hfing.ticketflowapi.service;

import com.hfing.ticketflowapi.dto.request.LoginRequest;
import com.hfing.ticketflowapi.dto.response.LoginResponse;

public interface AuthenticationService {
    LoginResponse login(LoginRequest request);
}
