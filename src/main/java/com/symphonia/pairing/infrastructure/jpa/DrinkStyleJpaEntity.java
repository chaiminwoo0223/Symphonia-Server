package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.entity.DrinkStyle;
import com.symphonia.pairing.domain.vo.DrinkCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "drink_style")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DrinkStyleJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DrinkCategory category;

    @Embedded private FlavorProfileEmbeddable flavorProfile;

    public static DrinkStyleJpaEntity from(DrinkStyle drinkStyle) {
        return DrinkStyleJpaEntity.builder()
                .id(drinkStyle.getId())
                .name(drinkStyle.getName())
                .category(drinkStyle.getCategory())
                .flavorProfile(FlavorProfileEmbeddable.from(drinkStyle.getFlavorProfile()))
                .build();
    }

    public DrinkStyle toDomain() {
        return DrinkStyle.builder()
                .id(id)
                .name(name)
                .category(category)
                .flavorProfile(flavorProfile.toDomain())
                .build();
    }
}
