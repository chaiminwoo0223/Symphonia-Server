package com.symphonia.catalog.domain.vo;

// 100g당 함량 구간 [minGrams, maxGrams)를 맛 값으로 바꾼다.
public record NutrientRule(double minGrams, double maxGrams, int value) {
    public boolean contains(double grams) {
        return grams >= minGrams && grams < maxGrams;
    }
}
