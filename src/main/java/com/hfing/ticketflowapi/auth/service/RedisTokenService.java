package com.hfing.ticketflowapi.auth.service;

import com.hfing.ticketflowapi.auth.entity.RedisToken;


public interface RedisTokenService {

    void saveToken(RedisToken token);

    void deleteTokenByJwtId(String jwtId);

    boolean existsByJwtId(String jwtId);
}