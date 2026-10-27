package com.symphonia.catalog.domain.vo;

public record WineBodyRule(String body, int richness) {
    public boolean matches(XWine wine) {
        return body.equals(wine.body());
    }
}
