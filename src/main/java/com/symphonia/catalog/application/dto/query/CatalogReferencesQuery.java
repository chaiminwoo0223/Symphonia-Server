package com.symphonia.catalog.application.dto.query;

public record CatalogReferencesQuery(Long drinkId, Long anjuId) {
    public static CatalogReferencesQuery of(Long drinkId, Long anjuId) {
        return new CatalogReferencesQuery(drinkId, anjuId);
    }
}
