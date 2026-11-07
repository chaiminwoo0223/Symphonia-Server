package com.symphonia.catalog.application.usecase;

import com.symphonia.catalog.application.dto.query.CatalogReferencesQuery;

public interface VerifyCatalogReferencesUseCase {
    void verify(CatalogReferencesQuery query);
}
