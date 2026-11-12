package com.symphonia.catalog.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RedisRepositoryTest;
import com.symphonia.catalog.application.dto.result.CatalogResult;
import com.symphonia.catalog.application.dto.result.CatalogResult.AnjuResult;
import com.symphonia.catalog.application.dto.result.CatalogResult.DrinkResult;
import com.symphonia.catalog.domain.vo.AllergyType;
import com.symphonia.catalog.domain.vo.AnjuBase;
import com.symphonia.catalog.domain.vo.DrinkCategory;
import com.symphonia.catalog.domain.vo.ServingStyle;
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

@DisplayName("CatalogCacheImpl 테스트")
class CatalogCacheImplTest extends RedisRepositoryTest {

    private static final String KEY = "catalog:v1";

    @Autowired private StringRedisTemplate redisTemplate;

    private CatalogCacheImpl catalogCache;

    // 슬라이스 테스트에는 JsonMapper 빈이 없어 직접 만든다.
    @BeforeEach
    void setUp() {
        catalogCache = new CatalogCacheImpl(redisTemplate, JsonMapper.builder().build());
    }

    @Nested
    @DisplayName("find 메서드는")
    class Find {

        @Test
        @DisplayName("저장한 카탈로그를 모든 필드와 함께 그대로 돌려준다")
        void shouldReturnPutCatalog() {
            // given
            CatalogResult catalog = catalog();
            catalogCache.put(catalog);

            // when
            Optional<CatalogResult> result = catalogCache.find();

            // then
            assertThat(result).contains(catalog);
        }

        @Test
        @DisplayName("캐시에 값이 없으면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenKeyNotExists() {
            // when
            Optional<CatalogResult> result = catalogCache.find();

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("값을 역직렬화하지 못하면 예외 없이 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenValueIsBroken() {
            // given
            redisTemplate.opsForValue().set(KEY, "깨진 값");

            // when
            Optional<CatalogResult> result = catalogCache.find();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("put 메서드는")
    class Put {

        @Test
        @DisplayName("이미 있는 값을 새 카탈로그로 덮어쓴다")
        void shouldOverwriteExistingCatalog() {
            // given
            catalogCache.put(catalog());
            CatalogResult updated = new CatalogResult(List.of(), List.of());

            // when
            catalogCache.put(updated);

            // then
            assertThat(catalogCache.find()).contains(updated);
        }

        @Test
        @DisplayName("만료 시간을 두고 저장한다")
        void shouldPutWithExpiration() {
            // when
            catalogCache.put(catalog());

            // then
            assertThat(redisTemplate.getExpire(KEY))
                    .isPositive()
                    .isLessThanOrEqualTo(Duration.ofHours(24).toSeconds());
        }
    }

    @Nested
    @DisplayName("evict 메서드는")
    class Evict {

        @Test
        @DisplayName("저장한 카탈로그를 지운다")
        void shouldRemoveCatalog() {
            // given
            catalogCache.put(catalog());

            // when
            catalogCache.evict();

            // then
            assertThat(catalogCache.find()).isEmpty();
        }
    }

    private CatalogResult catalog() {
        return new CatalogResult(
                List.of(
                        new DrinkResult(
                                1L, 10L, DrinkCategory.BEER, "카스", 4.5, false, 1, 2, 3, 4, 5, 6L),
                        new DrinkResult(2L, 11L, null, "스타일 없는 음료", 0.0, true, 0, 0, 0, 0, 0, 0L)),
                List.of(
                        new AnjuResult(
                                3L,
                                "견과류",
                                5,
                                4,
                                0,
                                2,
                                1,
                                Set.of(AllergyType.PEANUT, AllergyType.WALNUT),
                                AnjuBase.VEGETABLE,
                                ServingStyle.SNACK,
                                true,
                                7L)));
    }
}
