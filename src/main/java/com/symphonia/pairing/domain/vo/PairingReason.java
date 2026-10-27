package com.symphonia.pairing.domain.vo;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PairingReason {
    ABV_LIMIT("자리의 도수 제한에 맞는 술이에요", PairingReasonCriteria::abvLimit),
    ALLERGY_EXCLUDED("알레르기 유발 성분을 피했어요", PairingReasonCriteria::allergyExcluded),
    LIGHT_ANJU("가벼운 안주를 골랐어요", PairingReasonCriteria::lightAnju),
    NON_ALCOHOLIC_OPTION("무알코올 옵션이에요", PairingReasonCriteria::nonAlcoholicOption),
    FLAVOR_MATCH("술과 안주의 맛이 잘 어울려요", PairingReasonCriteria::flavorMatch),
    HARMONIZE_MATCH("이 술과 즐겨 곁들이는 음식이에요", PairingReasonCriteria::harmonizeMatch),
    RICHNESS_CUTTING("높은 도수가 안주의 기름진 맛을 깔끔하게 잡아줘요", PairingReasonCriteria::richnessCutting),
    CARBONATION_ACIDITY_CUTTING(
            "탄산이나 산미가 안주의 기름진 맛을 씻어줘요", PairingReasonCriteria::carbonationAcidityCutting),
    MOOD_MATCH("분위기와 잘 맞는 음악이에요", PairingReasonCriteria::moodMatch),
    ;

    private final String message;
    private final Predicate<PairingReasonContext> predicate;

    public String message() {
        return message;
    }

    private boolean appliesTo(PairingReasonContext context) {
        return predicate.test(context);
    }

    public static List<PairingReason> allApplicableTo(PairingReasonContext context) {
        return Arrays.stream(values()).filter(reason -> reason.appliesTo(context)).toList();
    }
}
