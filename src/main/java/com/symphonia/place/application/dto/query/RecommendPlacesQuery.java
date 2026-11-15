package com.symphonia.place.application.dto.query;

public record RecommendPlacesQuery(String region, Long anjuId) {
    public static RecommendPlacesQuery of(String region, Long anjuId) {
        return new RecommendPlacesQuery(region, anjuId);
    }
}
