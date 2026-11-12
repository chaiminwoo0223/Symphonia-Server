package com.symphonia.catalog.infrastructure.redis;

import com.symphonia.catalog.application.cache.CatalogCache;
import com.symphonia.catalog.application.dto.result.CatalogResult;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

// 키 이름의 v1은 값의 형식 버전이다. CatalogResult의 필드를 바꾸면 옛 값이 빈 값으로 읽혀도 오류가 나지 않으므로 버전을 올린다.
// 만료 시간은 마이그레이션처럼 적재 러너를 거치지 않는 변경이 캐시에 영구히 남지 않게 하는 안전망이다.
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCatalogCache implements CatalogCache {
    private static final String KEY = "catalog:v1";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final JsonMapper jsonMapper;

    @Override
    public Optional<CatalogResult> find() {
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(KEY)).map(this::toCatalog);
        } catch (DataAccessException | JacksonException e) {
            log.warn("카탈로그 캐시를 읽지 못해 DB에서 조회합니다. 원인: {}", e.toString());
            return Optional.empty();
        }
    }

    @Override
    public void put(CatalogResult catalog) {
        try {
            redisTemplate.opsForValue().set(KEY, jsonMapper.writeValueAsString(catalog), TTL);
        } catch (DataAccessException | JacksonException e) {
            log.warn("카탈로그 캐시를 저장하지 못했습니다. 원인: {}", e.toString());
        }
    }

    // 값이 JSON null이면 null을 돌려주고, 이 경우 map이 빈 Optional로 만든다.
    private CatalogResult toCatalog(String json) {
        return jsonMapper.readValue(json, CatalogResult.class);
    }
}
