package com.symphonia.pairing.fixture;

import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.vo.DrinkSource;
import com.symphonia.pairing.domain.vo.FlavorProfile;
import com.symphonia.pairing.domain.vo.Harmonize;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DrinkFixture {
    SOJU(
            DrinkStyleFixture.DISTILLED_SOJU,
            "소주",
            16.5,
            FlavorProfile.of(2, 2, 0, 1, 1),
            false,
            Set.of()),
    BEER(DrinkStyleFixture.ALE, "맥주", 4.5, FlavorProfile.of(1, 3, 4, 1, 2), false, Set.of()),
    WHISKEY(
            DrinkStyleFixture.BOURBON,
            "위스키",
            40.0,
            FlavorProfile.of(1, 4, 0, 4, 1),
            false,
            Set.of()),
    WINE(
            DrinkStyleFixture.PINOT_NOIR,
            "와인",
            13.0,
            FlavorProfile.of(2, 2, 0, 3, 4),
            false,
            Set.of()),
    SODA(DrinkStyleFixture.SODA, "탄산음료", 0.0, FlavorProfile.of(4, 0, 5, 0, 1), true, Set.of()),
    FRUIT_JUICE(
            DrinkStyleFixture.FRUIT_JUICE,
            "과일주스",
            0.0,
            FlavorProfile.of(4, 0, 0, 2, 3),
            true,
            Set.of()),
    // 골든 세트 음료다. 카탈로그에 적재된 제품의 이름, 도수, 맛, 음식 계열을 그대로 쓴다.
    // 카스 프레시의 음식 계열은 beer-harmonize.csv의 2A 규칙, Louis Jadot Beaujolais는 X-Wines Harmonize(Pasta,
    // Veal, Pork, Poultry)다.
    // 카스 프레시는 스타일(BJCP 2A) 맛을, Louis Jadot Beaujolais는 Red 맛에 Medium-bodied, High Acidity를 덮어쓴 맛을
    // 받는다.
    CASS_FRESH(
            DrinkStyleFixture.INTERNATIONAL_PALE_LAGER,
            "카스 프레시",
            4.5,
            FlavorProfile.of(2, 3, 5, 2, 1),
            false,
            Set.of(
                    Harmonize.POULTRY,
                    Harmonize.VEGETARIAN,
                    Harmonize.RICH_FISH,
                    Harmonize.CURED_MEAT)),
    LOUIS_JADOT_BEAUJOLAIS(
            DrinkStyleFixture.RED_WINE,
            "Louis Jadot Beaujolais",
            12.5,
            FlavorProfile.of(1, 3, 0, 3, 4),
            false,
            Set.of(Harmonize.PASTA, Harmonize.VEAL, Harmonize.PORK, Harmonize.POULTRY));

    private final DrinkStyleFixture drinkStyleFixture;
    private final String name;
    private final double abv;
    private final FlavorProfile flavorProfile;
    private final boolean nonAlcoholic;
    private final Set<Harmonize> harmonizes;

    // 시드의 external_id("soju" 등)와 (source, external_id) 유니크 제약이 겹치지 않게 접두어를 붙인다.
    public String getExternalId() {
        return "fixture-" + name().toLowerCase(Locale.ROOT);
    }

    public Drink create() {
        return build(null, defaultDrinkStyleId(), flavorProfile);
    }

    public Drink createWithId() {
        return createWithId(ordinal() + 1L);
    }

    public Drink createWithId(Long id) {
        return build(id, defaultDrinkStyleId(), flavorProfile);
    }

    public Drink createWithFlavorProfile(FlavorProfile profile) {
        return build(ordinal() + 1L, defaultDrinkStyleId(), profile);
    }

    // 음식 계열만 바꿔 Harmonize 점수와 추천 이유를 검증할 때 쓴다.
    public Drink createWithHarmonizes(Set<Harmonize> harmonizes) {
        return build(
                ordinal() + 1L, defaultDrinkStyleId(), getExternalId(), flavorProfile, harmonizes);
    }

    // FK가 걸린 drink style은 실제로 저장된 id를 받아 아직 저장 전인 음료를 만든다.
    public Drink createWithStyle(Long drinkStyleId) {
        return build(null, drinkStyleId, flavorProfile);
    }

    // 다시 적재할 때 같은 식별 키의 음료가 이 픽스처의 이름, 도수, 맛으로 바뀐 상황을 만든다.
    public Drink createWithStyleAndExternalId(Long drinkStyleId, String externalId) {
        return build(null, drinkStyleId, externalId, flavorProfile);
    }

    // 다시 적재할 때 같은 식별 키의 음료가 음식 계열만 바뀐 상황을 만든다.
    public Drink createWithStyleAndHarmonizes(Long drinkStyleId, Set<Harmonize> harmonizes) {
        return build(null, drinkStyleId, getExternalId(), flavorProfile, harmonizes);
    }

    // 단위 테스트에서 여러 음료가 같은 스타일을 공유하거나 서로 다른 스타일로 나뉘게 만들 때 쓴다.
    public Drink createWithIdAndStyle(Long id, Long drinkStyleId) {
        return build(id, drinkStyleId, flavorProfile);
    }

    // 저장하지 않는 단위 테스트에서는 스타일 픽스처의 고정 id를 참조한다.
    private Long defaultDrinkStyleId() {
        return drinkStyleFixture.createWithId().getId();
    }

    private Drink build(Long id, Long drinkStyleId, FlavorProfile profile) {
        return build(id, drinkStyleId, getExternalId(), profile);
    }

    private Drink build(Long id, Long drinkStyleId, String externalId, FlavorProfile profile) {
        return build(id, drinkStyleId, externalId, profile, harmonizes);
    }

    private Drink build(
            Long id,
            Long drinkStyleId,
            String externalId,
            FlavorProfile profile,
            Set<Harmonize> harmonizes) {
        return Drink.builder()
                .id(id)
                .drinkStyleId(drinkStyleId)
                .source(DrinkSource.SEED)
                .externalId(externalId)
                .name(name)
                .abv(abv)
                .flavorProfile(profile)
                .nonAlcoholic(nonAlcoholic)
                .harmonizes(harmonizes)
                .build();
    }
}
