package com.hfing.ticketflowapi.auth.repository;

import com.hfing.ticketflowapi.auth.entity.RedisToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface RedisTokenRepository extends CrudRepository<RedisToken, String> {
}