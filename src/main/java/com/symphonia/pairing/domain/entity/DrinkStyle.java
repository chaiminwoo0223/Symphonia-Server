package com.symphonia.pairing.domain.entity;

import com.symphonia.pairing.domain.vo.DrinkCategory;
import com.symphonia.pairing.domain.vo.DrinkStyleSource;
import com.symphonia.pairing.domain.vo.FlavorProfile;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DrinkStyle {
    private Long id;
    private DrinkStyleSource source;
    private String externalId;
    private String name;
    private DrinkCategory category;
    private FlavorProfile flavorProfile;

    public static DrinkStyle of(
            DrinkStyleSource source,
            String externalId,
            String name,
            DrinkCategory category,
            FlavorProfile flavorProfile) {
        return DrinkStyle.builder()
                .source(source)
                .externalId(externalId)
                .name(name)
                .category(category)
                .flavorProfile(flavorProfile)
                .build();
    }

    // 다시 적재할 때 원본에서 바뀔 수 있는 값만 덮어쓴다. id와 식별 키(source, externalId)는 유지한다.
    public DrinkStyle overwriteWith(DrinkStyle imported) {
        this.name = imported.name;
        this.category = imported.category;
        this.flavorProfile = imported.flavorProfile;
        return this;
    }
}
