package com.symphonia.pairing.fixture;

import com.symphonia.pairing.domain.entity.DrinkStyle;
import com.symphonia.pairing.domain.vo.DrinkCategory;
import com.symphonia.pairing.domain.vo.DrinkStyleSource;
import com.symphonia.pairing.domain.vo.FlavorProfile;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 와인 스타일은 X-Wines의 Type에서만 나오므로 출처는 X_WINES, 분류는 WINE으로 고정된다.
@Getter
@RequiredArgsConstructor
public enum WineStyleFixture {
    RED("red", "Red Wine", FlavorProfile.of(1, 3, 0, 2, 2));

    private final String externalId;
    private final String name;
    private final FlavorProfile flavorProfile;

    public DrinkStyle createWithId(Long id) {
        return build(id, name, flavorProfile);
    }

    // 이미 적재된 스타일을 흉내 내야 할 때 같은 식별 키에 이전 적재 당시의 이름과 맛을 채운다.
    public DrinkStyle createImportedStyle(Long id) {
        return build(id, "Old " + name, FlavorProfile.of(5, 5, 5, 5, 5));
    }

    private DrinkStyle build(Long id, String name, FlavorProfile flavorProfile) {
        return DrinkStyle.builder()
                .id(id)
                .source(DrinkStyleSource.X_WINES)
                .externalId(externalId)
                .name(name)
                .category(DrinkCategory.WINE)
                .flavorProfile(flavorProfile)
                .build();
    }
}
