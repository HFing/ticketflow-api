package com.hfing.ticketflowapi.service;

import com.hfing.ticketflowapi.entity.RedisToken;

public interface RedisTokenService {

    void saveToken(RedisToken token);

    void deleteTokenByJwtId(String jwtId);

    boolean existsByJwtId(String jwtId);
}