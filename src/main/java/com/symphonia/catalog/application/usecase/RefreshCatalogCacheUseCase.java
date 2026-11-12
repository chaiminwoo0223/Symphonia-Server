package com.symphonia.catalog.application.usecase;

import com.symphonia.catalog.application.dto.result.CatalogResult;

public interface RefreshCatalogCacheUseCase {
    // DB에서 전체 카탈로그를 읽어 캐시에 덮어쓰고, 읽은 카탈로그를 돌려준다.
    CatalogResult refresh();
}
