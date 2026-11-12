package com.symphonia.catalog.application.service;

import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.catalog.application.cache.CatalogCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("EvictCatalogCacheService 단위 테스트")
class EvictCatalogCacheServiceTest extends UnitTest {

    @InjectMocks private EvictCatalogCacheService evictCatalogCacheService;

    @Mock private CatalogCache catalogCache;

    @Nested
    @DisplayName("evict 메서드는")
    class Evict {

        @Test
        @DisplayName("카탈로그 캐시를 지운다")
        void shouldEvictCatalogCache() {
            // when
            evictCatalogCacheService.evict();

            // then
            then(catalogCache).should().evict();
        }
    }
}
