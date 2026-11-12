package com.symphonia.catalog.application.service;

import com.symphonia.catalog.application.cache.CatalogCache;
import com.symphonia.catalog.application.dto.query.CatalogQuery;
import com.symphonia.catalog.application.dto.query.CatalogReferencesQuery;
import com.symphonia.catalog.application.dto.result.CatalogReferencesResult;
import com.symphonia.catalog.application.dto.result.CatalogResult;
import com.symphonia.catalog.application.usecase.GetCatalogReferencesUseCase;
import com.symphonia.catalog.application.usecase.GetCatalogUseCase;
import com.symphonia.catalog.application.usecase.RefreshCatalogCacheUseCase;
import com.symphonia.catalog.application.usecase.VerifyCatalogReferencesUseCase;
import com.symphonia.catalog.domain.entity.Anju;
import com.symphonia.catalog.domain.entity.Drink;
import com.symphonia.catalog.domain.exception.AnjuNotFoundException;
import com.symphonia.catalog.domain.exception.DrinkNotFoundException;
import com.symphonia.catalog.domain.repository.AnjuRepository;
import com.symphonia.catalog.domain.repository.DrinkRepository;
import com.symphonia.catalog.domain.repository.DrinkStyleRepository;
import com.symphonia.common.annotation.QueryService;
import lombok.RequiredArgsConstructor;

@QueryService
@RequiredArgsConstructor
public class CatalogQueryService
        implements GetCatalogUseCase, VerifyCatalogReferencesUseCase, GetCatalogReferencesUseCase {
    private final DrinkRepository drinkRepository;
    private final DrinkStyleRepository drinkStyleRepository;
    private final AnjuRepository anjuRepository;
    private final CatalogCache catalogCache;
    private final RefreshCatalogCacheUseCase refreshCatalogCacheUseCase;

    // 전체 카탈로그를 캐시에서 읽고, 없으면 DB에서 읽어 캐시에 채운다(Cache-Aside).
    // 도수 상한은 읽은 카탈로그에서 메모리로 거른다. 상한마다 캐시 키를 나누지 않는다.
    @Override
    public CatalogResult get(CatalogQuery query) {
        return catalogCache
                .find()
                .orElseGet(refreshCatalogCacheUseCase::refresh)
                .filterByMaxAbv(query.maxAbv());
    }

    @Override
    public void verify(CatalogReferencesQuery query) {
        findDrink(query.drinkId());
        findAnju(query.anjuId());
    }

    @Override
    public CatalogReferencesResult getReferences(CatalogReferencesQuery query) {
        return CatalogReferencesResult.of(findDrink(query.drinkId()), findAnju(query.anjuId()));
    }

    private Drink findDrink(Long drinkId) {
        return drinkRepository.findById(drinkId).orElseThrow(DrinkNotFoundException::new);
    }

    private Anju findAnju(Long anjuId) {
        return anjuRepository.findById(anjuId).orElseThrow(AnjuNotFoundException::new);
    }
}
