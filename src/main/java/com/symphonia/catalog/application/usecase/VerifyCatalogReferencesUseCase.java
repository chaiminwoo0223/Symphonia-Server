package com.symphonia.catalog.application.usecase;

import com.symphonia.catalog.application.dto.query.VerifyCatalogReferencesQuery;

public interface VerifyCatalogReferencesUseCase {
    void verify(VerifyCatalogReferencesQuery query);
}
