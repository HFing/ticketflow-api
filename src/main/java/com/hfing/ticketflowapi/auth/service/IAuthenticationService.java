package com.hfing.ticketflowapi.auth.service;

import com.hfing.ticketflowapi.auth.dto.LoginRequest;
import com.hfing.ticketflowapi.auth.dto.LoginResponse;
import com.hfing.ticketflowapi.auth.dto.LoginResult;
import com.nimbusds.jose.JOSEException;
import java.text.ParseException;



public interface IAuthenticationService {
    LoginResult login(LoginRequest request);
    LoginResponse refreshToken(String refreshToken );
    void logout(String refreshToken) throws ParseException, JOSEException;
}