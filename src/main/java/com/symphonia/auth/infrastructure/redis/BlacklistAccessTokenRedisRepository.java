package com.symphonia.auth.infrastructure.redis;

import org.springframework.data.repository.CrudRepository;

public interface BlacklistAccessTokenRedisRepository extends CrudRepository<BlacklistAccessToken, String> {
}
