package com.symphonia.catalog.application.usecase;

import com.symphonia.catalog.application.dto.query.CatalogReferencesQuery;
import com.symphonia.catalog.application.dto.result.CatalogReferencesResult;

public interface GetCatalogReferencesUseCase {
    CatalogReferencesResult getReferences(CatalogReferencesQuery query);
}
