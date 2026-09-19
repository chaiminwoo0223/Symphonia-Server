package com.symphonia.pairing.domain.entity;

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
    private String name;
    private double abv;
    private FlavorProfile flavorProfile;
    private boolean nonAlcoholic;

    public static Drink of(
            String name, double abv, FlavorProfile flavorProfile, boolean nonAlcoholic) {
        return Drink.builder()
                .name(name)
                .abv(abv)
                .flavorProfile(flavorProfile)
                .nonAlcoholic(nonAlcoholic)
                .build();
    }
}
