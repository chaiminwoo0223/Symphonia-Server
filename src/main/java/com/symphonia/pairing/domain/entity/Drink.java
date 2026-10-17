package com.symphonia.pairing.domain.entity;

import com.symphonia.pairing.domain.vo.DrinkSource;
import com.symphonia.pairing.domain.vo.FlavorProfile;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Drink {
    private Long id;
    private Long drinkStyleId;
    private DrinkSource source;
    private String externalId;
    private String name;
    private double abv;
    private FlavorProfile flavorProfile;
    private boolean nonAlcoholic;

    public static Drink of(
            Long drinkStyleId,
            DrinkSource source,
            String externalId,
            String name,
            double abv,
            FlavorProfile flavorProfile,
            boolean nonAlcoholic) {
        return Drink.builder()
                .drinkStyleId(drinkStyleId)
                .source(source)
                .externalId(externalId)
                .name(name)
                .abv(abv)
                .flavorProfile(flavorProfile)
                .nonAlcoholic(nonAlcoholic)
                .build();
    }

    // 다시 적재할 때 원본에서 바뀔 수 있는 값만 덮어쓴다. id와 식별 키(source, externalId), 무알코올 여부는 유지한다.
    public Drink overwriteWith(Drink imported) {
        this.drinkStyleId = imported.drinkStyleId;
        this.name = imported.name;
        this.abv = imported.abv;
        this.flavorProfile = imported.flavorProfile;
        return this;
    }

    public double flavorSimilarityTo(Anju anju) {
        return flavorProfile.similarity(anju.getFlavorProfile());
    }

    public boolean matchesFlavorOf(Anju anju) {
        return flavorProfile.matches(anju.getFlavorProfile());
    }
}
