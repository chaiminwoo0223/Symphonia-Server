package com.symphonia.auth.infrastructure.redis;

import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRedisRepository extends CrudRepository<RefreshToken, String> {
}
