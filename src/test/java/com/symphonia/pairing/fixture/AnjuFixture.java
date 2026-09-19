package com.symphonia.pairing.fixture;

import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.vo.AllergyType;
import com.symphonia.pairing.domain.vo.FlavorProfile;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnjuFixture {
    GOLBAENGI_MUCHIM("골뱅이무침", FlavorProfile.of(3, 1, 0, 2), Set.of()),
    FRIED_CHICKEN("후라이드치킨", FlavorProfile.of(2, 0, 0, 5), Set.of()),
    DRIED_SNACK("마른안주", FlavorProfile.of(1, 1, 0, 1), Set.of()),
    LIGHT_BALANCED("가벼운 안주", FlavorProfile.of(2, 2, 2, 2), Set.of()),
    HEAVY_BALANCED("기름진 안주", FlavorProfile.of(2, 2, 2, 5), Set.of()),
    PEANUT_ALLERGY("땅콩 안주", FlavorProfile.of(2, 2, 2, 2), Set.of(AllergyType.PEANUT));

    private final String name;
    private final FlavorProfile flavorProfile;
    private final Set<AllergyType> allergyTypes;

    public Anju create() {
        return Anju.builder()
                .name(name)
                .flavorProfile(flavorProfile)
                .allergyTypes(allergyTypes)
                .build();
    }
}
