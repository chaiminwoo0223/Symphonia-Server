package com.symphonia.pairing.domain.vo;

final class PairingReasonCriteria {
    private PairingReasonCriteria() {}

    public static boolean abvLimit(PairingReasonContext context) {
        return context.occasion().hasAbvLimit();
    }

    public static boolean allergyExcluded(PairingReasonContext context) {
        return context.occasion().hasAllergyInput();
    }

    public static boolean lightAnju(PairingReasonContext context) {
        return context.occasion().prefersLightAnju() && context.anju().getFlavorProfile().isLight();
    }

    public static boolean nonAlcoholicOption(PairingReasonContext context) {
        return context.occasion().requiresNonAlcoholicOption() && context.drink().isNonAlcoholic();
    }

    public static boolean flavorMatch(PairingReasonContext context) {
        return context.drink().matchesFlavorOf(context.anju());
    }

    public static boolean harmonizeMatch(PairingReasonContext context) {
        return context.drink().harmonizesWith(context.anju());
    }

    public static boolean richnessCutting(PairingReasonContext context) {
        return context.drink().cutsRichnessOf(context.anju());
    }

    public static boolean carbonationAcidityCutting(PairingReasonContext context) {
        return context.drink().cutsRichnessWithCarbonationOrAcidityOf(context.anju());
    }

    public static boolean moodMatch(PairingReasonContext context) {
        return context.musicMood().fitsFor(context.occasion().toMoodProfile());
    }
}
