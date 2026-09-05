package com.symphonia.auth.infrastructure.redis;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRedisRepository extends CrudRepository<RefreshToken, String> {
    List<RefreshToken> findAllByMemberId(String memberId);
}
