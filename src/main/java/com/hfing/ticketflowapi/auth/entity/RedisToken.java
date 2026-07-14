package com.hfing.ticketflowapi.auth.entity;

import java.util.concurrent.TimeUnit;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;



@RedisHash("redis_token")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class RedisToken {

    @Id
    private String jwtId;

    @Indexed
    private String userId;

    @TimeToLive(unit = TimeUnit.SECONDS)
    private Long expiration;
}