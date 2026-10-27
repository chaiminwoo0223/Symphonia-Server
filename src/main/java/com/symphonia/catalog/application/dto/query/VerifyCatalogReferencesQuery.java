package com.symphonia.catalog.application.dto.query;

public record VerifyCatalogReferencesQuery(Long drinkId, Long anjuId, Long musicMoodId) {
    public static VerifyCatalogReferencesQuery of(Long drinkId, Long anjuId, Long musicMoodId) {
        return new VerifyCatalogReferencesQuery(drinkId, anjuId, musicMoodId);
    }
}
