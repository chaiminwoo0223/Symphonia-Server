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
    GOLBAENGI_MUCHIM("골뱅이무침", FlavorProfile.of(3, 1, 0, 2), Set.of(AllergyType.WHEAT)),
    FRIED_CHICKEN(
            "후라이드치킨",
            FlavorProfile.of(2, 0, 0, 5),
            Set.of(AllergyType.WHEAT, AllergyType.EGG, AllergyType.CHICKEN)),
    FRIED_SHRIMP(
            "새우튀김",
            FlavorProfile.of(2, 0, 0, 4),
            Set.of(AllergyType.SHRIMP, AllergyType.WHEAT, AllergyType.EGG)),
    TOFU_KIMCHI(
            "두부김치", FlavorProfile.of(1, 2, 0, 2), Set.of(AllergyType.SOYBEAN, AllergyType.PORK)),
    DRIED_SNACK(
            "마른안주", FlavorProfile.of(1, 1, 0, 1), Set.of(AllergyType.SQUID, AllergyType.PEANUT)),
    CHEESE_PLATTER("치즈플래터", FlavorProfile.of(2, 1, 0, 3), Set.of(AllergyType.MILK)),
    FRUIT_PLATTER("과일안주", FlavorProfile.of(4, 0, 0, 1), Set.of()),
    BOILED_PORK("수육", FlavorProfile.of(1, 0, 0, 4), Set.of(AllergyType.PORK)),
    FRENCH_FRIES("감자튀김", FlavorProfile.of(1, 0, 0, 4), Set.of());

    private final String name;
    private final FlavorProfile flavorProfile;
    private final Set<AllergyType> allergyTypes;

    public Anju.AnjuBuilder builder() {
        return Anju.builder().name(name).flavorProfile(flavorProfile).allergyTypes(allergyTypes);
    }

    public Anju create() {
        return builder().build();
    }

    public Anju createWithId() {
        return builder().id(ordinal() + 1L).build();
    }
}
