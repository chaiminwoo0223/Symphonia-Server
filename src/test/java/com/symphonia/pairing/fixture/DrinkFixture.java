package com.symphonia.pairing.fixture;

import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.vo.FlavorProfile;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DrinkFixture {
    SOJU("소주", 16.5, FlavorProfile.of(2, 2, 0, 1), false),
    BEER("맥주", 4.5, FlavorProfile.of(1, 3, 4, 1), false),
    WHISKEY("위스키", 40.0, FlavorProfile.of(1, 4, 0, 4), false),
    WINE("와인", 13.0, FlavorProfile.of(2, 2, 0, 3), false),
    SODA("탄산음료", 0.0, FlavorProfile.of(4, 0, 5, 0), true),
    FRUIT_JUICE("과일주스", 0.0, FlavorProfile.of(4, 0, 0, 2), true);

    private final String name;
    private final double abv;
    private final FlavorProfile flavorProfile;
    private final boolean nonAlcoholic;

    public Drink.DrinkBuilder builder() {
        return Drink.builder()
                .name(name)
                .abv(abv)
                .flavorProfile(flavorProfile)
                .nonAlcoholic(nonAlcoholic);
    }

    public Drink create() {
        return builder().build();
    }

    public Drink createWithId() {
        return builder().id(ordinal() + 1L).build();
    }
}
