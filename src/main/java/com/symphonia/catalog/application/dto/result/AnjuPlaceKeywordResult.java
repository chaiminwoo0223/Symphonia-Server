package com.symphonia.catalog.application.dto.result;

import com.symphonia.catalog.domain.entity.Anju;

// 안주를 파는 가게를 찾을 때 지역명과 함께 쓰는 검색어다.
public record AnjuPlaceKeywordResult(String placeKeyword) {
    public static AnjuPlaceKeywordResult from(Anju anju) {
        return new AnjuPlaceKeywordResult(anju.getPlaceKeyword());
    }
}
