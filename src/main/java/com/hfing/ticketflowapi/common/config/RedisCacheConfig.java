package com.hfing.ticketflowapi.common.config;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration(proxyBeanMethods = false)
public class RedisCacheConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {

        GenericJacksonJsonRedisSerializer jsonSerializer =
                GenericJacksonJsonRedisSerializer.builder()
                        .enableSpringCacheNullValueSupport()
                        .build();

        RedisCacheConfiguration cacheConfiguration =
                RedisCacheConfiguration.defaultCacheConfig()
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(jsonSerializer)
                        )
                        .disableCachingNullValues();

        return builder -> builder
                .withCacheConfiguration(
                        "eventsList",
                        cacheConfiguration.entryTtl(
                                randomTtl(
                                        Duration.ofMinutes(1),
                                        Duration.ofMinutes(2)
                                )
                        )
                )
                .withCacheConfiguration(
                        "publicEventDetail",
                        cacheConfiguration.entryTtl(
                                randomTtl(
                                        Duration.ofMinutes(1),
                                        Duration.ofMinutes(2)
                                )
                        )
                )
                .withCacheConfiguration(
                        "adminEventDetail",
                        cacheConfiguration.entryTtl(
                                randomTtl(
                                        Duration.ofMinutes(30),
                                        Duration.ofMinutes(35)
                                )
                        )
                );
    }

    private RedisCacheWriter.TtlFunction randomTtl(
            Duration min,
            Duration max
    ) {
        return (key, value) -> Duration.ofMillis(
                ThreadLocalRandom.current()
                        .nextLong(min.toMillis(), max.toMillis() + 1)
        );
    }
}