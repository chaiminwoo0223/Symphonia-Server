package com.symphonia.catalog.application.dto.query;

public record CatalogQuery(double maxAbv) {
    public static CatalogQuery of(double maxAbv) {
        return new CatalogQuery(maxAbv);
    }
}
