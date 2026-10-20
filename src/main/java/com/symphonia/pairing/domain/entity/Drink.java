package com.symphonia.pairing.domain.entity;

import com.symphonia.pairing.domain.vo.DrinkSource;
import com.symphonia.pairing.domain.vo.FlavorProfile;
import com.symphonia.pairing.domain.vo.Harmonize;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Drink {
    // 희석식 소주(16~17%)와 증류주, 주정강화 와인부터 해당한다. 일반 와인은 대부분 15% 미만이다.
    private static final double RICHNESS_CUTTING_ABV = 15.0;

    private Long id;
    private Long drinkStyleId;
    private DrinkSource source;
    private String externalId;
    private String name;
    private double abv;
    private FlavorProfile flavorProfile;
    private boolean nonAlcoholic;
    private Set<Harmonize> harmonizes;

    @Builder
    private Drink(
            Long id,
            Long drinkStyleId,
            DrinkSource source,
            String externalId,
            String name,
            double abv,
            FlavorProfile flavorProfile,
            boolean nonAlcoholic,
            Set<Harmonize> harmonizes) {
        this.id = id;
        this.drinkStyleId = drinkStyleId;
        this.source = source;
        this.externalId = externalId;
        this.name = name;
        this.abv = abv;
        this.flavorProfile = flavorProfile;
        this.nonAlcoholic = nonAlcoholic;
        this.harmonizes = harmonizes == null ? Set.of() : Set.copyOf(harmonizes);
    }

    public static Drink of(
            Long drinkStyleId,
            DrinkSource source,
            String externalId,
            String name,
            double abv,
            FlavorProfile flavorProfile,
            boolean nonAlcoholic,
            Set<Harmonize> harmonizes) {
        return Drink.builder()
                .drinkStyleId(drinkStyleId)
                .source(source)
                .externalId(externalId)
                .name(name)
                .abv(abv)
                .flavorProfile(flavorProfile)
                .nonAlcoholic(nonAlcoholic)
                .harmonizes(harmonizes)
                .build();
    }

    // 다시 적재할 때 원본에서 바뀔 수 있는 값만 덮어쓴다. id와 식별 키(source, externalId), 무알코올 여부는 유지한다.
    public Drink overwriteWith(Drink imported) {
        this.drinkStyleId = imported.drinkStyleId;
        this.name = imported.name;
        this.abv = imported.abv;
        this.flavorProfile = imported.flavorProfile;
        this.harmonizes = imported.harmonizes;
        return this;
    }

    public double flavorSimilarityTo(Anju anju) {
        return flavorProfile.similarity(anju.getFlavorProfile());
    }

    public boolean matchesFlavorOf(Anju anju) {
        return flavorProfile.matches(anju.getFlavorProfile());
    }

    // 이 술에 어울리는 음식 계열(Harmonize) 중 하나라도 안주에 맞는지 본다.
    // 요청마다 음료×안주 쌍 전체에서 불리므로 스트림을 만들지 않고 반복문으로 둔다.
    public boolean harmonizesWith(Anju anju) {
        for (Harmonize harmonize : harmonizes) {
            if (harmonize.matches(anju)) {
                return true;
            }
        }
        return false;
    }

    // 도수가 높은 술은 무게감 있는 안주의 기름기를 씻어 준다. 가벼운 안주에는 씻어 낼 기름기가 없다.
    public boolean cutsRichnessOf(Anju anju) {
        return abv >= RICHNESS_CUTTING_ABV && !anju.getFlavorProfile().isLight();
    }
}
