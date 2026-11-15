package com.symphonia.place.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RedisRepositoryTest;
import com.symphonia.common.crypto.Sha256;
import com.symphonia.place.domain.vo.Place;
import com.symphonia.place.domain.vo.PlaceSearchQuery;
import com.symphonia.place.fixture.PlaceFixture;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("RedisPlaceSearchCache 테스트")
class RedisPlaceSearchCacheTest extends RedisRepositoryTest {

    private static final PlaceSearchQuery QUERY = PlaceSearchQuery.of("강남구", "호프");
    private static final String KEY = "place:v1:" + Sha256.hash(QUERY.text());

    @Autowired private StringRedisTemplate redisTemplate;

    private RedisPlaceSearchCache placeSearchCache;

    // 슬라이스 테스트에는 JsonMapper 빈이 없어 직접 만든다.
    @BeforeEach
    void setUp() {
        placeSearchCache = new RedisPlaceSearchCache(redisTemplate, JsonMapper.builder().build());
    }

    @Nested
    @DisplayName("find 메서드는")
    class Find {

        @Test
        @DisplayName("저장한 가게 목록을 순서와 모든 필드 그대로 돌려준다")
        void shouldReturnPutPlacesInOrder() {
            // given
            List<Place> places =
                    List.of(PlaceFixture.GANGNAM_HOF.create(), PlaceFixture.NONHYEON_PUB.create());
            placeSearchCache.put(QUERY, places);

            // when
            Optional<List<Place>> result = placeSearchCache.find(QUERY);

            // then
            assertThat(result).contains(places);
        }

        @Test
        @DisplayName("캐시에 값이 없으면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenKeyNotExists() {
            // when
            Optional<List<Place>> result = placeSearchCache.find(QUERY);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("결과가 없던 검색은 캐시에 없는 것과 구분해 빈 목록으로 돌려준다")
        void shouldReturnEmptyListWhenEmptyPlacesPut() {
            // given
            placeSearchCache.put(QUERY, List.of());

            // when
            Optional<List<Place>> result = placeSearchCache.find(QUERY);

            // then
            assertThat(result).contains(List.of());
        }

        @Test
        @DisplayName("검색어가 다르면 서로의 값을 읽지 않는다")
        void shouldNotReturnPlacesOfOtherQuery() {
            // given
            placeSearchCache.put(QUERY, List.of(PlaceFixture.GANGNAM_HOF.create()));

            // when
            Optional<List<Place>> result =
                    placeSearchCache.find(PlaceSearchQuery.of("강남구", "이자카야"));

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("값을 역직렬화하지 못하면 예외 없이 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenValueIsBroken() {
            // given
            redisTemplate.opsForValue().set(KEY, "깨진 값");

            // when
            Optional<List<Place>> result = placeSearchCache.find(QUERY);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("값이 JSON null이면 예외 없이 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenValueIsJsonNull() {
            // given
            redisTemplate.opsForValue().set(KEY, "null");

            // when
            Optional<List<Place>> result = placeSearchCache.find(QUERY);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("put 메서드는")
    class Put {

        @Test
        @DisplayName("10분 이내의 만료 시간을 두고 저장한다")
        void shouldPutWithExpiration() {
            // when
            placeSearchCache.put(QUERY, List.of(PlaceFixture.GANGNAM_HOF.create()));

            // then
            assertThat(redisTemplate.getExpire(KEY))
                    .isPositive()
                    .isLessThanOrEqualTo(Duration.ofMinutes(10).toSeconds());
        }

        @Test
        @DisplayName("지역명과 검색어를 읽을 수 있는 글자로 키에 남기지 않는다")
        void shouldNotLeaveRegionInKey() {
            // when
            placeSearchCache.put(QUERY, List.of(PlaceFixture.GANGNAM_HOF.create()));

            // then
            Set<String> keys = redisTemplate.keys("place:v1:*");
            assertThat(keys).containsExactly(KEY);
            assertThat(keys).noneMatch(key -> key.contains("강남구") || key.contains("호프"));
        }
    }
}
