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

    public void update(String name, DrinkCategory category, FlavorProfile flavorProfile) {
        this.name = name;
        this.category = category;
        this.flavorProfile = flavorProfile;
    }
}
