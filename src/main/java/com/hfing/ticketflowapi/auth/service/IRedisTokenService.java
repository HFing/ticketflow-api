package com.hfing.ticketflowapi.auth.service;

import com.hfing.ticketflowapi.auth.entity.RedisToken;

public interface IRedisTokenService {
    String ACCESS_TOKEN_BLACKLIST_PREFIX = "access:blacklist:";

    void saveToken(RedisToken token);

    void deleteTokenByJwtId(String jwtId);

    boolean existsByJwtId(String jwtId);
}
