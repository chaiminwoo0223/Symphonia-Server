package com.symphonia.catalog.application.service;

import com.symphonia.catalog.application.cache.CatalogCache;
import com.symphonia.catalog.application.usecase.EvictCatalogCacheUseCase;
import com.symphonia.common.annotation.CommandService;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class EvictCatalogCacheService implements EvictCatalogCacheUseCase {
    private final CatalogCache catalogCache;

    @Override
    public void evict() {
        catalogCache.evict();
    }
}
