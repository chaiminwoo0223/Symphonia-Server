package com.symphonia.pairing.fixture;

import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.vo.FlavorProfile;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DrinkFixture {
    SOJU("소주", 16.5, FlavorProfile.of(2, 2, 0, 1)),
    LOW_ABV_BEER("저도수 맥주", 4.5, FlavorProfile.of(1, 3, 4, 1)),
    WHISKEY("위스키", 40.0, FlavorProfile.of(1, 4, 0, 4)),
    BALANCED("밸런스 술", 5.0, FlavorProfile.of(2, 2, 2, 2));

    private final String name;
    private final double abv;
    private final FlavorProfile flavorProfile;

    public Drink create() {
        return Drink.builder().name(name).abv(abv).flavorProfile(flavorProfile).build();
    }
}
