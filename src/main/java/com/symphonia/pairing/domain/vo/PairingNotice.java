package com.symphonia.pairing.domain.vo;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;

// 추천이 비었거나 요청한 무알코올 음료를 찾지 못했을 때 그 사유를 알린다.
@RequiredArgsConstructor
public enum PairingNotice {
    NO_DRINK_WITHIN_ABV_LIMIT(
            "자리의 도수 제한에 맞는 술이 없어서 추천할 수 없어요", PairingNoticeCriteria::noDrinkWithinAbvLimit),
    NO_ANJU_WITHOUT_ALLERGEN(
            "알레르기 유발 성분을 피한 안주가 없어서 추천할 수 없어요", PairingNoticeCriteria::noAnjuWithoutAllergen),
    NO_NON_ALCOHOLIC_DRINK("무알코올 음료를 찾지 못했어요", PairingNoticeCriteria::noNonAlcoholicDrink),
    ;

    private final String message;
    private final Predicate<PairingNoticeContext> predicate;

    public String message() {
        return message;
    }

    private boolean appliesTo(PairingNoticeContext context) {
        return predicate.test(context);
    }

    public static List<PairingNotice> allApplicableTo(PairingNoticeContext context) {
        return Arrays.stream(values()).filter(notice -> notice.appliesTo(context)).toList();
    }
}
