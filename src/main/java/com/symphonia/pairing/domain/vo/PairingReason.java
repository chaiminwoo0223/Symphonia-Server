package com.symphonia.pairing.domain.vo;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PairingReason {
    ABV_LIMIT("자리의 도수 제한에 맞는 술이에요") {
        @Override
        boolean appliesTo(PairingCandidate candidate) {
            return candidate.occasion().hasAbvLimit();
        }
    },
    ALLERGY_EXCLUDED("알레르기 유발 성분을 피했어요") {
        @Override
        boolean appliesTo(PairingCandidate candidate) {
            return candidate.occasion().hasAllergyInput();
        }
    },
    LIGHT_ANJU("가벼운 안주를 골랐어요") {
        @Override
        boolean appliesTo(PairingCandidate candidate) {
            return candidate.occasion().prefersLightAnju()
                    && candidate.anju().getFlavorProfile().isLight();
        }
    },
    NON_ALCOHOLIC_OPTION("무알코올 옵션이에요") {
        @Override
        boolean appliesTo(PairingCandidate candidate) {
            return candidate.occasion().requiresNonAlcoholicOption()
                    && candidate.drink().isNonAlcoholic();
        }
    },
    FLAVOR_MATCH("술과 안주의 맛이 잘 어울려요") {
        @Override
        boolean appliesTo(PairingCandidate candidate) {
            return candidate
                    .drink()
                    .getFlavorProfile()
                    .matches(candidate.anju().getFlavorProfile());
        }
    },
    MOOD_MATCH("분위기와 잘 맞는 음악이에요") {
        @Override
        boolean appliesTo(PairingCandidate candidate) {
            return candidate
                    .musicMood()
                    .getMoodProfile()
                    .fits(candidate.occasion().toMoodProfile());
        }
    },
    ;

    private final String message;

    public String message() {
        return message;
    }

    abstract boolean appliesTo(PairingCandidate candidate);

    public static List<PairingReason> allApplicableTo(PairingCandidate candidate) {
        return Arrays.stream(values()).filter(reason -> reason.appliesTo(candidate)).toList();
    }
}
