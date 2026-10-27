package com.symphonia.pairing.domain.vo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 추천이 점수를 매기는 음료다. 카탈로그의 음료 중 추천에 필요한 값만 담는다.
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PairingDrink {
    // 희석식 소주(16~17%)와 증류주, 주정강화 와인부터 해당한다. 일반 와인은 대부분 15% 미만이다.
    private static final double RICHNESS_CUTTING_ABV = 15.0;

    private final Long id;
    private final Long drinkStyleId;
    private final String name;
    private final double abv;
    private final boolean nonAlcoholic;
    private final FlavorProfile flavorProfile;

    @Getter(AccessLevel.NONE)
    private final long harmonizeMask;

    public static PairingDrink of(
            Long id,
            Long drinkStyleId,
            String name,
            double abv,
            boolean nonAlcoholic,
            FlavorProfile flavorProfile,
            long harmonizeMask) {
        return new PairingDrink(
                id, drinkStyleId, name, abv, nonAlcoholic, flavorProfile, harmonizeMask);
    }

    public double flavorSimilarityTo(PairingAnju anju) {
        return flavorProfile.similarity(anju.getFlavorProfile());
    }

    public boolean matchesFlavorOf(PairingAnju anju) {
        return flavorProfile.matches(anju.getFlavorProfile());
    }

    // 이 술에 어울리는 음식 계열(Harmonize) 중 하나라도 안주에 맞는지 본다.
    // 요청마다 음료×안주 쌍 전체에서 불리므로 카탈로그가 미리 계산한 비트 집합끼리 비교한다.
    public boolean harmonizesWith(PairingAnju anju) {
        return (harmonizeMask & anju.getHarmonizeMask()) != 0;
    }

    // 도수가 높은 술은 무게감 있는 안주의 기름기를 씻어 준다. 가벼운 안주에는 씻어 낼 기름기가 없다.
    public boolean cutsRichnessOf(PairingAnju anju) {
        return abv >= RICHNESS_CUTTING_ABV && !anju.getFlavorProfile().isLight();
    }

    // 탄산이나 산미가 강한 술도 무게감 있는 안주의 기름기를 씻어 준다.
    // 도수로 이미 씻어 주는 술은 같은 효과를 두 번 세지 않도록 도수 쪽만 본다.
    public boolean cutsRichnessWithCarbonationOrAcidityOf(PairingAnju anju) {
        return flavorProfile.cutsRichness()
                && !anju.getFlavorProfile().isLight()
                && !cutsRichnessOf(anju);
    }
}
