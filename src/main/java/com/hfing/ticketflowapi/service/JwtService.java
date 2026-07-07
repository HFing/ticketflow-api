package com.hfing.ticketflowapi.service;

import com.hfing.ticketflowapi.dto.TokenDetails;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;

import java.text.ParseException;
import java.util.Set;

public interface JwtService {
    String generateAccessToken(String userId, String role);
    SignedJWT validateToken (String token) throws ParseException, JOSEException;
    TokenDetails generateRefreshToken(String userId);
}
