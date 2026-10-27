package com.symphonia.pairing.domain.vo;

import com.symphonia.catalog.domain.entity.Anju;
import com.symphonia.catalog.domain.entity.Drink;

// 추천 후보로 고른 음료×안주 쌍과 그 쌍의 점수다.
public record ScoredDrinkAnju(Drink drink, Anju anju, double score) {}
