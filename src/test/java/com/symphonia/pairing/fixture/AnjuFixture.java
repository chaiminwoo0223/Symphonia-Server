package com.symphonia.pairing.fixture;

import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.vo.AllergyType;
import com.symphonia.pairing.domain.vo.AnjuBase;
import com.symphonia.pairing.domain.vo.AnjuSource;
import com.symphonia.pairing.domain.vo.FlavorProfile;
import com.symphonia.pairing.domain.vo.ServingStyle;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnjuFixture {
    GOLBAENGI_MUCHIM(
            "골뱅이무침",
            FlavorProfile.of(3, 1, 0, 2, 4),
            Set.of(AllergyType.WHEAT),
            AnjuBase.SEAFOOD,
            ServingStyle.MAIN,
            true),
    FRIED_CHICKEN(
            "후라이드치킨",
            FlavorProfile.of(2, 0, 0, 5, 0),
            Set.of(AllergyType.WHEAT, AllergyType.EGG, AllergyType.CHICKEN),
            AnjuBase.POULTRY,
            ServingStyle.MAIN,
            false),
    FRIED_SHRIMP(
            "새우튀김",
            FlavorProfile.of(2, 0, 0, 4, 0),
            Set.of(AllergyType.SHRIMP, AllergyType.WHEAT, AllergyType.EGG),
            AnjuBase.SEAFOOD,
            ServingStyle.APPETIZER,
            false),
    TOFU_KIMCHI(
            "두부김치",
            FlavorProfile.of(1, 2, 0, 2, 2),
            Set.of(AllergyType.SOYBEAN, AllergyType.PORK),
            AnjuBase.PORK,
            ServingStyle.MAIN,
            true),
    DRIED_SNACK(
            "마른안주",
            FlavorProfile.of(1, 1, 0, 1, 0),
            Set.of(AllergyType.SQUID, AllergyType.PEANUT),
            AnjuBase.SEAFOOD,
            ServingStyle.SNACK,
            false),
    CHEESE_PLATTER(
            "치즈플래터",
            FlavorProfile.of(2, 1, 0, 3, 1),
            Set.of(AllergyType.MILK),
            AnjuBase.HARD_CHEESE,
            ServingStyle.APPETIZER,
            false),
    FRUIT_PLATTER(
            "과일안주",
            FlavorProfile.of(4, 0, 0, 1, 3),
            Set.of(),
            AnjuBase.SWEET,
            ServingStyle.SNACK,
            false),
    BOILED_PORK(
            "수육",
            FlavorProfile.of(1, 0, 0, 4, 0),
            Set.of(AllergyType.PORK),
            AnjuBase.PORK,
            ServingStyle.MAIN,
            false),
    FRENCH_FRIES(
            "감자튀김",
            FlavorProfile.of(1, 0, 0, 4, 0),
            Set.of(),
            AnjuBase.VEGETABLE,
            ServingStyle.SNACK,
            false);

    private final String name;
    private final FlavorProfile flavorProfile;
    private final Set<AllergyType> allergyTypes;
    private final AnjuBase base;
    private final ServingStyle serving;
    private final boolean spicy;

    // 시드의 external_id("fried-chicken" 등)와 (source, external_id) 유니크 제약이 겹치지 않게 접두어를 붙인다.
    public String getExternalId() {
        return "fixture-" + name().toLowerCase(Locale.ROOT);
    }

    public Anju create() {
        return build(null, AnjuSource.SEED, getExternalId());
    }

    public Anju createWithId() {
        return createWithId(ordinal() + 1L);
    }

    public Anju createWithId(Long id) {
        return build(id, AnjuSource.SEED, getExternalId());
    }

    public Anju createWithIdentity(AnjuSource source, String externalId) {
        return build(null, source, externalId);
    }

    private Anju build(Long id, AnjuSource source, String externalId) {
        return Anju.builder()
                .id(id)
                .source(source)
                .externalId(externalId)
                .name(name)
                .flavorProfile(flavorProfile)
                .allergyTypes(allergyTypes)
                .base(base)
                .serving(serving)
                .spicy(spicy)
                .build();
    }
}
