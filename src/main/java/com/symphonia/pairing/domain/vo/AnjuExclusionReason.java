package com.symphonia.pairing.domain.vo;

// 매핑표의 안주를 적재하지 못한 이유다. 한 안주에 여러 이유가 함께 걸릴 수 있다.
public enum AnjuExclusionReason {
    MISSING_EXTERNAL_ID,
    MISSING_KFIND_CODE,
    EXTERNAL_ID_MISMATCH,
    NUTRITION_NOT_FOUND,
    SUGAR_NOT_MEASURED,
    FAT_NOT_MEASURED,
    UNKNOWN_INGREDIENT,
}
