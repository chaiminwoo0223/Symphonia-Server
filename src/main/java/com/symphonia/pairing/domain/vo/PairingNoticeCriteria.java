package com.symphonia.pairing.domain.vo;

final class PairingNoticeCriteria {
    private PairingNoticeCriteria() {}

    public static boolean noDrinkWithinAbvLimit(PairingNoticeContext context) {
        return !context.recommended()
                && context.occasion().hasAbvLimit()
                && !context.hasDrinkWithinAbvLimit();
    }

    public static boolean noAnjuWithoutAllergen(PairingNoticeContext context) {
        return !context.recommended()
                && context.occasion().hasAllergyInput()
                && !context.hasAnjuWithoutAllergen();
    }

    public static boolean noNonAlcoholicDrink(PairingNoticeContext context) {
        return context.occasion().requiresNonAlcoholicOption() && !context.hasNonAlcoholicPairing();
    }
}
