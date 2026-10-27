package com.symphonia.catalog.application.service;

import com.symphonia.catalog.application.dto.query.CatalogQuery;
import com.symphonia.catalog.application.dto.query.VerifyCatalogReferencesQuery;
import com.symphonia.catalog.application.dto.result.CatalogResult;
import com.symphonia.catalog.application.usecase.GetCatalogUseCase;
import com.symphonia.catalog.application.usecase.VerifyCatalogReferencesUseCase;
import com.symphonia.catalog.domain.exception.AnjuNotFoundException;
import com.symphonia.catalog.domain.exception.DrinkNotFoundException;
import com.symphonia.catalog.domain.exception.MusicMoodNotFoundException;
import com.symphonia.catalog.domain.repository.AnjuRepository;
import com.symphonia.catalog.domain.repository.DrinkRepository;
import com.symphonia.catalog.domain.repository.MusicMoodRepository;
import com.symphonia.common.annotation.QueryService;
import lombok.RequiredArgsConstructor;

@QueryService
@RequiredArgsConstructor
public class CatalogQueryService implements GetCatalogUseCase, VerifyCatalogReferencesUseCase {
    private final DrinkRepository drinkRepository;
    private final AnjuRepository anjuRepository;
    private final MusicMoodRepository musicMoodRepository;

    // 도수 상한을 넘는 음료는 조회 단계에서 먼저 걸러 읽는 행을 줄인다.
    @Override
    public CatalogResult get(CatalogQuery query) {
        return CatalogResult.of(
                drinkRepository.findAllByAbvLessThanEqual(query.maxAbv()),
                anjuRepository.findAll(),
                musicMoodRepository.findAll());
    }

    @Override
    public void verify(VerifyCatalogReferencesQuery query) {
        drinkRepository.findById(query.drinkId()).orElseThrow(DrinkNotFoundException::new);
        anjuRepository.findById(query.anjuId()).orElseThrow(AnjuNotFoundException::new);
        musicMoodRepository
                .findById(query.musicMoodId())
                .orElseThrow(MusicMoodNotFoundException::new);
    }
}
