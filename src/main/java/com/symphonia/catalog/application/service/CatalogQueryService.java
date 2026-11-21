package com.symphonia.catalog.application.service;

import com.symphonia.catalog.application.cache.CatalogCache;
import com.symphonia.catalog.application.dto.query.CatalogQuery;
import com.symphonia.catalog.application.dto.query.CatalogReferencesQuery;
import com.symphonia.catalog.application.dto.result.AnjuPlaceKeywordResult;
import com.symphonia.catalog.application.dto.result.CatalogReferencesResult;
import com.symphonia.catalog.application.dto.result.CatalogResult;
import com.symphonia.catalog.application.usecase.GetAnjuPlaceKeywordUseCase;
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
import com.symphonia.common.annotation.QueryService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@QueryService
@RequiredArgsConstructor
public class CatalogQueryService
        implements GetCatalogUseCase,
                VerifyCatalogReferencesUseCase,
                GetCatalogReferencesUseCase,
                GetAnjuPlaceKeywordUseCase {
    private final DrinkRepository drinkRepository;
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

    // 쌍마다 따로 읽으면 쌍 수만큼 조회하므로, 음료와 안주를 id 모음으로 한 번씩 읽어 쌍에 맞춘다.
    @Override
    public List<CatalogReferencesResult> getAllReferences(List<CatalogReferencesQuery> queries) {
        Map<Long, Drink> drinksById =
                drinkRepository
                        .findAllByIds(
                                queries.stream()
                                        .map(CatalogReferencesQuery::drinkId)
                                        .distinct()
                                        .toList())
                        .stream()
                        .collect(Collectors.toMap(Drink::getId, Function.identity()));
        Map<Long, Anju> anjusById =
                anjuRepository
                        .findAllByIds(
                                queries.stream()
                                        .map(CatalogReferencesQuery::anjuId)
                                        .distinct()
                                        .toList())
                        .stream()
                        .collect(Collectors.toMap(Anju::getId, Function.identity()));

        return queries.stream()
                .map(
                        query ->
                                CatalogReferencesResult.of(
                                        Optional.ofNullable(drinksById.get(query.drinkId()))
                                                .orElseThrow(DrinkNotFoundException::new),
                                        Optional.ofNullable(anjusById.get(query.anjuId()))
                                                .orElseThrow(AnjuNotFoundException::new)))
                .toList();
    }

    @Override
    public AnjuPlaceKeywordResult getPlaceKeyword(Long anjuId) {
        return AnjuPlaceKeywordResult.from(findAnju(anjuId));
    }

    private Drink findDrink(Long drinkId) {
        return drinkRepository.findById(drinkId).orElseThrow(DrinkNotFoundException::new);
    }

    private Anju findAnju(Long anjuId) {
        return anjuRepository.findById(anjuId).orElseThrow(AnjuNotFoundException::new);
    }
}
