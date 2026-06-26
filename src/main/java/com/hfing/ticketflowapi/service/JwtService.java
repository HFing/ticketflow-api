package com.hfing.ticketflowapi.service;

import java.util.Set;

public interface JwtService {
    String generateAccessToken(String userId, String role);
    String generateRefreshToken(String userId);
}
