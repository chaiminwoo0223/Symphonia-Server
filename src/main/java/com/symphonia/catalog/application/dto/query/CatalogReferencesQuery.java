package com.symphonia.catalog.application.dto.query;

public record CatalogReferencesQuery(Long drinkId, Long anjuId, Long musicMoodId) {
    public static CatalogReferencesQuery of(Long drinkId, Long anjuId, Long musicMoodId) {
        return new CatalogReferencesQuery(drinkId, anjuId, musicMoodId);
    }
}
