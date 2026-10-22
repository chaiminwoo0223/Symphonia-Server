package com.symphonia.pairing.domain.vo;

final class PairingReasonCriteria {
    private PairingReasonCriteria() {}

    public static boolean abvLimit(PairingCandidate candidate) {
        return candidate.occasion().hasAbvLimit();
    }

    public static boolean allergyExcluded(PairingCandidate candidate) {
        return candidate.occasion().hasAllergyInput();
    }

    public static boolean lightAnju(PairingCandidate candidate) {
        return candidate.occasion().prefersLightAnju()
                && candidate.anju().getFlavorProfile().isLight();
    }

    public static boolean nonAlcoholicOption(PairingCandidate candidate) {
        return candidate.occasion().requiresNonAlcoholicOption()
                && candidate.drink().isNonAlcoholic();
    }

    public static boolean flavorMatch(PairingCandidate candidate) {
        return candidate.drink().matchesFlavorOf(candidate.anju());
    }

    public static boolean harmonizeMatch(PairingCandidate candidate) {
        return candidate.drink().harmonizesWith(candidate.anju());
    }

    public static boolean richnessCutting(PairingCandidate candidate) {
        return candidate.drink().cutsRichnessOf(candidate.anju());
    }

    public static boolean carbonationAcidityCutting(PairingCandidate candidate) {
        return candidate.drink().cutsRichnessWithCarbonationOrAcidityOf(candidate.anju());
    }

    public static boolean moodMatch(PairingCandidate candidate) {
        return candidate.musicMood().fitsFor(candidate.occasion().toMoodProfile());
    }
}
