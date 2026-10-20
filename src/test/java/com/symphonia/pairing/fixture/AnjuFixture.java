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
            false),
    // CATALOG_ 상수는 골든 세트 안주다. 맛, 알레르기는 anjus.csv와 K-FIND 영양성분을 적재 규칙으로 계산한 값이다.
    // 위의 FRIED_CHICKEN, CHEESE_PLATTER는 V2 시드 값이라 실제 카탈로그 값과 다르다.
    CATALOG_SAMGYEOPSAL_GUI(
            "삼겹살구이",
            FlavorProfile.of(1, 0, 0, 5, 0),
            Set.of(AllergyType.PORK),
            AnjuBase.PORK,
            ServingStyle.MAIN,
            false),
    CATALOG_FRIED_CHICKEN(
            "후라이드치킨",
            FlavorProfile.of(1, 0, 0, 4, 0),
            Set.of(AllergyType.CHICKEN, AllergyType.EGG, AllergyType.WHEAT),
            AnjuBase.POULTRY,
            ServingStyle.MAIN,
            false),
    CATALOG_CHEESE_PLATTER(
            "치즈플래터",
            FlavorProfile.of(1, 0, 0, 3, 0),
            Set.of(AllergyType.MILK, AllergyType.WHEAT),
            AnjuBase.HARD_CHEESE,
            ServingStyle.APPETIZER,
            false),
    // 아래는 현재 점수 규칙에서 골든 세트 음료별 카탈로그 상위 3위 안주다. 동점이면 anjus.csv에서 앞선 안주를 고른다.
    // 소주: 도토리묵무침, 닭꼬치, 찜닭 / 카스 프레시: 도토리묵무침, 닭강정, 닭똥집볶음 / Louis Jadot Beaujolais: 훈제연어, 깐풍기, 두부김치
    CATALOG_DOTORIMUK_MUCHIM(
            "도토리묵무침",
            FlavorProfile.of(1, 2, 0, 1, 0),
            Set.of(AllergyType.SOYBEAN, AllergyType.WHEAT),
            AnjuBase.VEGETABLE,
            ServingStyle.APPETIZER,
            false),
    CATALOG_DAK_KKOCHI(
            "닭꼬치",
            FlavorProfile.of(2, 0, 0, 1, 0),
            Set.of(AllergyType.CHICKEN, AllergyType.SOYBEAN, AllergyType.WHEAT),
            AnjuBase.POULTRY,
            ServingStyle.APPETIZER,
            false),
    CATALOG_JJIMDAK(
            "찜닭",
            FlavorProfile.of(2, 0, 0, 1, 0),
            Set.of(AllergyType.CHICKEN, AllergyType.SOYBEAN, AllergyType.WHEAT),
            AnjuBase.POULTRY,
            ServingStyle.MAIN,
            false),
    CATALOG_DAKGANGJEONG(
            "닭강정",
            FlavorProfile.of(2, 0, 0, 2, 0),
            Set.of(AllergyType.CHICKEN, AllergyType.PEANUT, AllergyType.SOYBEAN, AllergyType.WHEAT),
            AnjuBase.POULTRY,
            ServingStyle.MAIN,
            true),
    CATALOG_DAKTTONGJIP_BOKKEUM(
            "닭똥집볶음",
            FlavorProfile.of(2, 0, 0, 2, 0),
            Set.of(AllergyType.CHICKEN),
            AnjuBase.POULTRY,
            ServingStyle.MAIN,
            false),
    CATALOG_SMOKED_SALMON(
            "훈제연어",
            FlavorProfile.of(1, 0, 0, 3, 3),
            Set.of(),
            AnjuBase.RICH_FISH,
            ServingStyle.APPETIZER,
            false),
    CATALOG_KKANPUNGGI(
            "깐풍기",
            FlavorProfile.of(2, 0, 0, 2, 4),
            Set.of(AllergyType.CHICKEN, AllergyType.SOYBEAN, AllergyType.WHEAT),
            AnjuBase.POULTRY,
            ServingStyle.MAIN,
            true),
    CATALOG_TOFU_KIMCHI(
            "두부김치",
            FlavorProfile.of(1, 0, 0, 2, 3),
            Set.of(AllergyType.PORK, AllergyType.SOYBEAN),
            AnjuBase.PORK,
            ServingStyle.MAIN,
            true);

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
