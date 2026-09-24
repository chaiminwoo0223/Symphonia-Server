package com.symphonia.pairing.domain.vo;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public enum PairingReason {
    ABV_LIMIT("자리의 도수 제한에 맞는 술이에요", PairingReasonCriteria::abvLimit),
    ALLERGY_EXCLUDED("알레르기 유발 성분을 피했어요", PairingReasonCriteria::allergyExcluded),
    LIGHT_ANJU("가벼운 안주를 골랐어요", PairingReasonCriteria::lightAnju),
    NON_ALCOHOLIC_OPTION("무알코올 옵션이에요", PairingReasonCriteria::nonAlcoholicOption),
    FLAVOR_MATCH("술과 안주의 맛이 잘 어울려요", PairingReasonCriteria::flavorMatch),
    MOOD_MATCH("분위기와 잘 맞는 음악이에요", PairingReasonCriteria::moodMatch),
    ;

    private final String message;
    private final Predicate<PairingCandidate> predicate;

    PairingReason(String message, Predicate<PairingCandidate> predicate) {
        this.message = message;
        this.predicate = predicate;
    }

    public String message() {
        return message;
    }

    private boolean appliesTo(PairingCandidate candidate) {
        return predicate.test(candidate);
    }

    public static List<PairingReason> allApplicableTo(PairingCandidate candidate) {
        return Arrays.stream(values()).filter(reason -> reason.appliesTo(candidate)).toList();
    }
}
