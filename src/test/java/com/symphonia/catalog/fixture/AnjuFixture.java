package com.symphonia.catalog.fixture;

import com.symphonia.catalog.domain.entity.Anju;
import com.symphonia.catalog.domain.vo.AllergyType;
import com.symphonia.catalog.domain.vo.AnjuBase;
import com.symphonia.catalog.domain.vo.AnjuSource;
import com.symphonia.catalog.domain.vo.FlavorProfile;
import com.symphonia.catalog.domain.vo.ServingStyle;
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
    // 아래 주석의 순위는 그 안주를 더한 시점의 규칙으로 계산했다.
    // 맛 값은 #117의 형태 보정과 100ml 환산을 반영한 현재 값이다.
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
    // 아래는 도수 대비, Harmonize 점수를 넣기 전 규칙에서 골든 세트 음료별 카탈로그 상위 3위 안주다. 동점이면 anjus.csv에서 앞선 안주를 고른다.
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
            FlavorProfile.of(2, 0, 0, 3, 4),
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
            true),
    // 아래는 도수 대비, Harmonize 점수를 넣은 뒤의 카탈로그 순위로 더한 안주다.
    // 카스 프레시는 도토리묵무침이 1위, 닭강정과 닭똥집볶음이 동점 2위로 위에서 이미 더한 안주와 같다.
    // Louis Jadot Beaujolais는 깐풍기, 두부김치, 김치찌개, 탕수육이 동점 1위이고, Harmonize에 맞지 않는 안주 중 1위는 훈제연어, 2위는
    // 부대찌개다.
    CATALOG_KIMCHI_JJIGAE(
            "김치찌개",
            FlavorProfile.of(1, 0, 0, 2, 3),
            Set.of(AllergyType.PORK, AllergyType.SOYBEAN),
            AnjuBase.PORK,
            ServingStyle.MAIN,
            true),
    CATALOG_TANGSUYUK(
            "탕수육",
            FlavorProfile.of(3, 0, 0, 2, 4),
            Set.of(AllergyType.PORK, AllergyType.EGG, AllergyType.WHEAT),
            AnjuBase.PORK,
            ServingStyle.MAIN,
            false),
    CATALOG_BUDAE_JJIGAE(
            "부대찌개",
            FlavorProfile.of(1, 0, 0, 2, 3),
            Set.of(AllergyType.MILK, AllergyType.PORK, AllergyType.WHEAT),
            AnjuBase.CURED_MEAT,
            ServingStyle.MAIN,
            true),
    // 소주(음식 계열 PORK, BEEF)는 돼지껍데기구이가 1위, 소막창구이, 대창구이, 삼겹살구이, 고추장삼겹살이 동점 2위다.
    // 곱창전골은 음식 계열에 맞지만 가벼운 안주 중 1위, 고다치즈는 무게감이 높지만 음식 계열에 맞지 않는 안주다.
    CATALOG_DWAEJI_KKEOPDEGI_GUI(
            "돼지껍데기구이",
            FlavorProfile.of(2, 0, 0, 5, 0),
            Set.of(AllergyType.PORK, AllergyType.SOYBEAN),
            AnjuBase.PORK,
            ServingStyle.APPETIZER,
            false),
    CATALOG_SOMAKCHANG_GUI(
            "소막창구이",
            FlavorProfile.of(1, 0, 0, 5, 0),
            Set.of(AllergyType.BEEF, AllergyType.SOYBEAN),
            AnjuBase.BEEF,
            ServingStyle.MAIN,
            false),
    CATALOG_DAECHANG_GUI(
            "대창구이",
            FlavorProfile.of(1, 0, 0, 5, 0),
            Set.of(AllergyType.BEEF),
            AnjuBase.BEEF,
            ServingStyle.MAIN,
            false),
    CATALOG_GOCHUJANG_SAMGYEOPSAL(
            "고추장삼겹살",
            FlavorProfile.of(3, 0, 0, 5, 0),
            Set.of(AllergyType.PORK, AllergyType.SOYBEAN, AllergyType.WHEAT),
            AnjuBase.PORK,
            ServingStyle.MAIN,
            true),
    CATALOG_GOPCHANG_JEONGOL(
            "곱창전골",
            FlavorProfile.of(1, 0, 0, 2, 0),
            Set.of(AllergyType.BEEF, AllergyType.SOYBEAN),
            AnjuBase.BEEF,
            ServingStyle.MAIN,
            true),
    CATALOG_GOUDA_CHEESE(
            "고다치즈",
            FlavorProfile.of(1, 0, 0, 4, 0),
            Set.of(AllergyType.MILK, AllergyType.WHEAT),
            AnjuBase.HARD_CHEESE,
            ServingStyle.SNACK,
            false),
    // 아래는 탄산을 유사도에서 빼고 탄산, 산미 대비 점수를 넣은 뒤의 카탈로그 순위로 더한 안주다.
    // 카스 프레시(음식 계열 POULTRY, VEGETARIAN, RICH_FISH, CURED_MEAT)는 이 네 안주가 동점 1위다.
    // 도토리묵무침은 음식 계열에 맞지만 가벼운 안주 중 1위, 돼지껍데기구이와 고다치즈는 무게감이 높지만 음식 계열에 맞지 않는 안주다.
    CATALOG_POPCORN(
            "팝콘",
            FlavorProfile.of(1, 0, 0, 4, 0),
            Set.of(AllergyType.MILK),
            AnjuBase.VEGETABLE,
            ServingStyle.SNACK,
            false),
    CATALOG_ROASTED_PEANUTS(
            "볶음땅콩",
            FlavorProfile.of(3, 0, 0, 4, 0),
            Set.of(AllergyType.PEANUT),
            AnjuBase.VEGETABLE,
            ServingStyle.SNACK,
            false),
    CATALOG_MIXED_NUTS(
            "견과류모둠",
            FlavorProfile.of(3, 0, 0, 4, 0),
            Set.of(AllergyType.WALNUT),
            AnjuBase.VEGETABLE,
            ServingStyle.SNACK,
            false),
    CATALOG_COLD_CUT_PLATTER(
            "콜드컷플래터",
            FlavorProfile.of(1, 0, 0, 5, 0),
            Set.of(AllergyType.PORK, AllergyType.WHEAT),
            AnjuBase.CURED_MEAT,
            ServingStyle.APPETIZER,
            false),
    // 아래는 집어 먹는 안주의 무게감을 1 낮춘 뒤(#117)의 카탈로그 순위로 더한 안주다.
    // 카스 프레시는 콜드컷플래터가 1위, 도토리묵무침이 2위, 장어구이와 핫도그가 동점 3위다.
    // 핫도그와 감자튀김은 튀긴 요리라 형태 보정을 받지 않는다.
    // 팝콘, 볶음땅콩, 견과류모둠은 무게감 4로 후라이드치킨, 감자튀김과 동점 5위다.
    CATALOG_JANGEO_GUI(
            "장어구이",
            FlavorProfile.of(2, 0, 0, 4, 0),
            Set.of(AllergyType.SOYBEAN, AllergyType.WHEAT),
            AnjuBase.RICH_FISH,
            ServingStyle.MAIN,
            false),
    CATALOG_HOTDOG(
            "핫도그",
            FlavorProfile.of(2, 0, 0, 4, 2),
            Set.of(AllergyType.PORK, AllergyType.WHEAT, AllergyType.TOMATO),
            AnjuBase.CURED_MEAT,
            ServingStyle.SNACK,
            false),
    CATALOG_FRENCH_FRIES(
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
