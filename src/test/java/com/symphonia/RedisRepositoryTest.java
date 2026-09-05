package com.symphonia;

import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataRedisTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class RedisRepositoryTest {}
