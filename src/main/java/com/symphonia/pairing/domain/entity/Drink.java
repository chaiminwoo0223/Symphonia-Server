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

    public void update(Long drinkStyleId, String name, double abv, FlavorProfile flavorProfile) {
        this.drinkStyleId = drinkStyleId;
        this.name = name;
        this.abv = abv;
        this.flavorProfile = flavorProfile;
    }
}
