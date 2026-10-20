package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.vo.Harmonize;

// drink_harmonize의 한 행이다. 추천 조회는 음료 엔티티의 컬렉션을 채우지 않고 이 행만 따로 읽는다.
public record DrinkHarmonizeRow(Long drinkId, Harmonize harmonize) {}
