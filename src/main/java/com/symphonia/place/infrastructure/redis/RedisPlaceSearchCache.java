package com.symphonia.place.infrastructure.redis;

import com.symphonia.common.crypto.Sha256;
import com.symphonia.place.application.cache.PlaceSearchCache;
import com.symphonia.place.domain.vo.Place;
import com.symphonia.place.domain.vo.PlaceSearchQuery;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

// 키 이름의 v1은 값의 형식 버전이다. Place의 필드를 바꾸면 옛 값이 빈 값으로 읽혀도 오류가 나지 않으므로 버전을 올린다.
// 키에는 검색어를 해시한 값을 쓴다. 사용자가 보낸 지역명이 Redis에 읽을 수 있는 글자로 남지 않게 하기 위해서다.
// 약관이 가게 정보를 별도로 복사해 두는 것을 금지하므로 짧은 만료 시간 안에서만 담는다.
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPlaceSearchCache implements PlaceSearchCache {
    private static final String KEY_PREFIX = "place:v1:";
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final TypeReference<List<Place>> PLACES_TYPE = new TypeReference<>() {};

    private final StringRedisTemplate redisTemplate;
    private final JsonMapper jsonMapper;

    @Override
    public Optional<List<Place>> find(PlaceSearchQuery query) {
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(keyOf(query)))
                    .map(this::toPlaces);
        } catch (DataAccessException | JacksonException e) {
            log.warn("맛집 검색 캐시를 읽지 못해 외부 검색을 호출합니다. 원인: {}", e.toString());
            return Optional.empty();
        }
    }

    @Override
    public void put(PlaceSearchQuery query, List<Place> places) {
        try {
            redisTemplate
                    .opsForValue()
                    .set(keyOf(query), jsonMapper.writeValueAsString(places), TTL);
        } catch (DataAccessException | JacksonException e) {
            log.warn("맛집 검색 캐시를 저장하지 못했습니다. 원인: {}", e.toString());
        }
    }

    private String keyOf(PlaceSearchQuery query) {
        return KEY_PREFIX + Sha256.hash(query.text());
    }

    // 값이 JSON null이면 null을 돌려주고, 이 경우 map이 빈 Optional로 만든다.
    private List<Place> toPlaces(String json) {
        return jsonMapper.readValue(json, PLACES_TYPE);
    }
}
