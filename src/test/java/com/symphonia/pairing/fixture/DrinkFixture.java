package com.symphonia.pairing.fixture;

import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.vo.DrinkSource;
import com.symphonia.pairing.domain.vo.FlavorProfile;
import java.util.Locale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DrinkFixture {
    SOJU(DrinkStyleFixture.DISTILLED_SOJU, "소주", 16.5, FlavorProfile.of(2, 2, 0, 1, 1), false),
    BEER(DrinkStyleFixture.ALE, "맥주", 4.5, FlavorProfile.of(1, 3, 4, 1, 2), false),
    WHISKEY(DrinkStyleFixture.BOURBON, "위스키", 40.0, FlavorProfile.of(1, 4, 0, 4, 1), false),
    WINE(DrinkStyleFixture.PINOT_NOIR, "와인", 13.0, FlavorProfile.of(2, 2, 0, 3, 4), false),
    SODA(DrinkStyleFixture.SODA, "탄산음료", 0.0, FlavorProfile.of(4, 0, 5, 0, 1), true),
    FRUIT_JUICE(DrinkStyleFixture.FRUIT_JUICE, "과일주스", 0.0, FlavorProfile.of(4, 0, 0, 2, 3), true);

    private final DrinkStyleFixture drinkStyleFixture;
    private final String name;
    private final double abv;
    private final FlavorProfile flavorProfile;
    private final boolean nonAlcoholic;

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

    // FK가 걸린 drink style은 실제로 저장된 id를 받아 아직 저장 전인 음료를 만든다.
    public Drink createWithStyle(Long drinkStyleId) {
        return build(null, drinkStyleId, flavorProfile);
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
        return Drink.builder()
                .id(id)
                .drinkStyleId(drinkStyleId)
                .source(DrinkSource.SEED)
                .externalId(getExternalId())
                .name(name)
                .abv(abv)
                .flavorProfile(profile)
                .nonAlcoholic(nonAlcoholic)
                .build();
    }
}
