package com.symphonia.catalog.application.cache;

import com.symphonia.catalog.application.dto.result.CatalogResult;
import java.util.Optional;

// 도수 상한으로 거르기 전의 전체 카탈로그를 하나의 값으로 담는다.
// 캐시 장애는 구현체가 삼키고, find는 캐시에 없을 때와 같은 빈 값을 돌려준다.
public interface CatalogCache {
    Optional<CatalogResult> find();

    void put(CatalogResult catalog);
}
