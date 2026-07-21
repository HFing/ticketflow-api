package com.hfing.ticketflowapi.auth.service.impl;

import com.hfing.ticketflowapi.auth.entity.RedisToken;
import com.hfing.ticketflowapi.auth.repository.RedisTokenRepository;
import com.hfing.ticketflowapi.auth.service.IRedisTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class RedisTokenServiceImpl implements IRedisTokenService {

    private final RedisTokenRepository redisTokenRepository;

    @Override
    public void saveToken(RedisToken token) {
        redisTokenRepository.save(token);
    }

    @Override
    public void deleteTokenByJwtId(String jwtId) {
        redisTokenRepository.findById(jwtId)
                .ifPresent(redisTokenRepository::delete);
    }

    @Override
    public boolean existsByJwtId(String jwtId) {
        return redisTokenRepository.existsById(jwtId);
    }
}