package com.symphonia.catalog.application.usecase;

import com.symphonia.catalog.application.dto.result.AnjuPlaceKeywordResult;

public interface GetAnjuPlaceKeywordUseCase {
    AnjuPlaceKeywordResult getPlaceKeyword(Long anjuId);
}
