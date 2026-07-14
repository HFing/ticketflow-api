package com.hfing.ticketflowapi.auth.service;

import com.hfing.ticketflowapi.auth.dto.TokenDetails;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;



public interface JwtService {
    String generateAccessToken(String userId, String role);
    SignedJWT validateToken (String token) throws ParseException, JOSEException;
    TokenDetails generateRefreshToken(String userId);
}