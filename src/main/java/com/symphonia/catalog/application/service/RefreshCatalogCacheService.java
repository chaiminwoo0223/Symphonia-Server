package com.symphonia.catalog.application.service;

import com.symphonia.catalog.application.cache.CatalogCache;
import com.symphonia.catalog.application.dto.result.CatalogResult;
import com.symphonia.catalog.application.usecase.RefreshCatalogCacheUseCase;
import com.symphonia.catalog.domain.repository.AnjuRepository;
import com.symphonia.catalog.domain.repository.DrinkRepository;
import com.symphonia.catalog.domain.repository.DrinkStyleRepository;
import com.symphonia.common.annotation.QueryService;
import lombok.RequiredArgsConstructor;

// DB는 읽기만 하므로 읽기 전용 트랜잭션이다. 캐시 쓰기는 이 트랜잭션과 무관하다.
@QueryService
@RequiredArgsConstructor
public class RefreshCatalogCacheService implements RefreshCatalogCacheUseCase {
    private final DrinkRepository drinkRepository;
    private final DrinkStyleRepository drinkStyleRepository;
    private final AnjuRepository anjuRepository;
    private final CatalogCache catalogCache;

    @Override
    public CatalogResult refresh() {
        CatalogResult catalog =
                CatalogResult.of(
                        drinkRepository.findAll(),
                        drinkStyleRepository.findAll(),
                        anjuRepository.findAll());

        catalogCache.put(catalog);

        return catalog;
    }
}
