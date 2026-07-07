package com.hfing.ticketflowapi.service;

import com.hfing.ticketflowapi.dto.request.LoginRequest;
import com.hfing.ticketflowapi.dto.response.LoginResponse;
import com.hfing.ticketflowapi.dto.response.LoginResult;
import com.nimbusds.jose.JOSEException;

import java.text.ParseException;

public interface AuthenticationService {
    LoginResult login(LoginRequest request);
    LoginResponse refreshToken(String refreshToken );
    void logout(String refreshToken) throws ParseException, JOSEException;
}
