package com.symphonia.pairing.domain.entity;

import com.symphonia.pairing.domain.vo.DrinkCategory;
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
    private String name;
    private DrinkCategory category;
    private FlavorProfile flavorProfile;

    public static DrinkStyle of(String name, DrinkCategory category, FlavorProfile flavorProfile) {
        return DrinkStyle.builder()
                .name(name)
                .category(category)
                .flavorProfile(flavorProfile)
                .build();
    }
}
