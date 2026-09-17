package com.symphonia.pairing.fixture;

import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.vo.FlavorProfile;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnjuFixture {
    GOLBAENGI_MUCHIM("골뱅이무침", FlavorProfile.of(3, 1, 0, 2)),
    FRIED_CHICKEN("후라이드치킨", FlavorProfile.of(2, 0, 0, 5)),
    DRIED_SNACK("마른안주", FlavorProfile.of(1, 1, 0, 1)),
    LIGHT_BALANCED("가벼운 안주", FlavorProfile.of(2, 2, 2, 2)),
    HEAVY_BALANCED("기름진 안주", FlavorProfile.of(2, 2, 2, 5));

    private final String name;
    private final FlavorProfile flavorProfile;

    public Anju create() {
        return Anju.builder().name(name).flavorProfile(flavorProfile).build();
    }
}
