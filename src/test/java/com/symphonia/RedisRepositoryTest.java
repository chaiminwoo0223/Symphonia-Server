package com.symphonia;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataRedisTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class RedisRepositoryTest {

    @Autowired private StringRedisTemplate redisTemplate;

    // Redis 컨테이너는 테스트 JVM 하나에서 IntegrationTest와 함께 쓴다(TestcontainersConfiguration).
    // 테스트마다 비워야 남은 토큰이 다른 테스트에 보이지 않는다.
    @AfterEach
    void flushRedis() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }
}
