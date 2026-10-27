package com.symphonia.catalog.application.usecase;

import com.symphonia.catalog.application.dto.query.CatalogQuery;
import com.symphonia.catalog.application.dto.result.CatalogResult;

public interface GetCatalogUseCase {
    CatalogResult get(CatalogQuery query);
}
