package com.symphonia.catalog.fixture;

import com.symphonia.catalog.domain.entity.DrinkStyle;
import com.symphonia.catalog.domain.vo.DrinkCategory;
import com.symphonia.catalog.domain.vo.DrinkStyleSource;
import com.symphonia.catalog.domain.vo.FlavorProfile;
import java.util.Locale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 시드 마이그레이션이 남아 있는 테스트에서도 name 유니크 제약에 걸리지 않게 시드에 없는 스타일 이름을 쓴다.
@Getter
@RequiredArgsConstructor
public enum DrinkStyleFixture {
    DISTILLED_SOJU("증류식 소주", DrinkCategory.SOJU, FlavorProfile.of(2, 2, 0, 2, 1)),
    ALE("에일", DrinkCategory.BEER, FlavorProfile.of(2, 3, 3, 2, 2)),
    BOURBON("버번 위스키", DrinkCategory.WHISKY, FlavorProfile.of(2, 3, 0, 4, 1)),
    PINOT_NOIR("피노 누아", DrinkCategory.WINE, FlavorProfile.of(2, 2, 0, 3, 4)),
    SODA("탄산음료", DrinkCategory.NON_ALCOHOLIC, FlavorProfile.of(4, 0, 5, 0, 1)),
    FRUIT_JUICE("과일주스", DrinkCategory.NON_ALCOHOLIC, FlavorProfile.of(4, 0, 0, 2, 3)),
    // 골든 세트 음료의 스타일이다. 맛은 BJCP 2A와 wine-type.csv Red를 적재 규칙으로 계산한 값이다.
    INTERNATIONAL_PALE_LAGER(
            "International Pale Lager", DrinkCategory.BEER, FlavorProfile.of(2, 3, 5, 2, 1)),
    RED_WINE("Red Wine", DrinkCategory.WINE, FlavorProfile.of(1, 3, 0, 4, 3));

    private final String name;
    private final DrinkCategory category;
    private final FlavorProfile flavorProfile;

    // 시드의 external_id("lager" 등)와 (source, external_id) 유니크 제약이 겹치지 않게 접두어를 붙인다.
    public String getExternalId() {
        return "fixture-" + name().toLowerCase(Locale.ROOT);
    }

    public DrinkStyle create() {
        return build(null, DrinkStyleSource.SEED, getExternalId());
    }

    public DrinkStyle createWithId() {
        return build(ordinal() + 1L, DrinkStyleSource.SEED, getExternalId());
    }

    // (source, external_id)는 스타일을 식별하는 유니크 키다. 적재나 유니크 제약을 검증할 때 이 키를 직접 정한다.
    public DrinkStyle createWithIdentity(DrinkStyleSource source, String externalId) {
        return build(null, source, externalId);
    }

    // 이미 저장된 스타일을 흉내 내야 할 때 id까지 채운다.
    public DrinkStyle createWithIdAndIdentity(DrinkStyleSource source, String externalId) {
        return build(ordinal() + 1L, source, externalId);
    }

    private DrinkStyle build(Long id, DrinkStyleSource source, String externalId) {
        return DrinkStyle.builder()
                .id(id)
                .source(source)
                .externalId(externalId)
                .name(name)
                .category(category)
                .flavorProfile(flavorProfile)
                .build();
    }
}
