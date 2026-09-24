package com.symphonia.pairing.domain.vo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PairingReason {
    ABV_LIMIT("자리의 도수 제한에 맞는 술이에요"),
    LIGHT_ANJU("가벼운 안주를 골랐어요"),
    NON_ALCOHOLIC_OPTION("무알코올 옵션이에요"),
    ALLERGY_EXCLUDED("알레르기 유발 성분을 피했어요"),
    FLAVOR_MATCH("술과 안주의 맛이 잘 어울려요"),
    MOOD_MATCH("분위기와 잘 맞는 음악이에요"),
    ;

    private final String message;

    public String message() {
        return message;
    }
}
