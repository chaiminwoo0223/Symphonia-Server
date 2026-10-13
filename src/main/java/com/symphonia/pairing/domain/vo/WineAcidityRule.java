package com.symphonia.pairing.domain.vo;

public record WineAcidityRule(String acidity, int value) {
    public boolean matches(XWine wine) {
        return acidity.equals(wine.acidity());
    }
}
