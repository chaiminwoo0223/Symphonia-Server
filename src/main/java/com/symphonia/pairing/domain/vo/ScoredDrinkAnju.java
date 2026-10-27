package com.symphonia.pairing.domain.vo;

// 추천 후보로 고른 음료×안주 쌍과 그 쌍의 점수다.
public record ScoredDrinkAnju(PairingDrink drink, PairingAnju anju, double score) {}
