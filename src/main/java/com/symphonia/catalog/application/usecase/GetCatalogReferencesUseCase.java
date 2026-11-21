package com.symphonia.catalog.application.usecase;

import com.symphonia.catalog.application.dto.query.CatalogReferencesQuery;
import com.symphonia.catalog.application.dto.result.CatalogReferencesResult;
import java.util.List;

public interface GetCatalogReferencesUseCase {
    CatalogReferencesResult getReferences(CatalogReferencesQuery query);

    // 여러 쌍을 음료와 안주 조회 한 번씩으로 읽는다. 결과는 요청한 순서와 같고, 없는 대상이 하나라도 있으면 예외를 던진다.
    List<CatalogReferencesResult> getAllReferences(List<CatalogReferencesQuery> queries);
}
