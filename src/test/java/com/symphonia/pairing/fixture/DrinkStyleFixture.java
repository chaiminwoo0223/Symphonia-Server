package com.symphonia.pairing.fixture;

import com.symphonia.pairing.domain.entity.DrinkStyle;
import com.symphonia.pairing.domain.vo.DrinkCategory;
import com.symphonia.pairing.domain.vo.FlavorProfile;
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
    FRUIT_JUICE("과일주스", DrinkCategory.NON_ALCOHOLIC, FlavorProfile.of(4, 0, 0, 2, 3));

    private final String name;
    private final DrinkCategory category;
    private final FlavorProfile flavorProfile;

    public DrinkStyle create() {
        return build(null);
    }

    public DrinkStyle createWithId() {
        return build(ordinal() + 1L);
    }

    private DrinkStyle build(Long id) {
        return DrinkStyle.builder()
                .id(id)
                .name(name)
                .category(category)
                .flavorProfile(flavorProfile)
                .build();
    }
}
