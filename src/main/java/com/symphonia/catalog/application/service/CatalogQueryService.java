package com.symphonia.catalog.application.service;

import com.symphonia.catalog.application.dto.query.CatalogQuery;
import com.symphonia.catalog.application.dto.query.CatalogReferencesQuery;
import com.symphonia.catalog.application.dto.result.CatalogReferencesResult;
import com.symphonia.catalog.application.dto.result.CatalogResult;
import com.symphonia.catalog.application.usecase.GetCatalogReferencesUseCase;
import com.symphonia.catalog.application.usecase.GetCatalogUseCase;
import com.symphonia.catalog.application.usecase.VerifyCatalogReferencesUseCase;
import com.symphonia.catalog.domain.entity.Anju;
import com.symphonia.catalog.domain.entity.Drink;
import com.symphonia.catalog.domain.exception.AnjuNotFoundException;
import com.symphonia.catalog.domain.exception.DrinkNotFoundException;
import com.symphonia.catalog.domain.repository.AnjuRepository;
import com.symphonia.catalog.domain.repository.DrinkRepository;
import com.symphonia.common.annotation.QueryService;
import lombok.RequiredArgsConstructor;

@QueryService
@RequiredArgsConstructor
public class CatalogQueryService
        implements GetCatalogUseCase, VerifyCatalogReferencesUseCase, GetCatalogReferencesUseCase {
    private final DrinkRepository drinkRepository;
    private final AnjuRepository anjuRepository;

    // 도수 상한을 넘는 음료는 조회 단계에서 먼저 걸러 읽는 행을 줄인다.
    @Override
    public CatalogResult get(CatalogQuery query) {
        return CatalogResult.of(
                drinkRepository.findAllByAbvLessThanEqual(query.maxAbv()),
                anjuRepository.findAll());
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
