package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.entity.DrinkStyle;
import com.symphonia.pairing.domain.vo.DrinkCategory;
import com.symphonia.pairing.domain.vo.DrinkStyleSource;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DrinkStyleSource source;

    @Column(nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DrinkCategory category;

    @Embedded private FlavorProfileEmbeddable flavorProfile;

    public static DrinkStyleJpaEntity from(DrinkStyle drinkStyle) {
        return DrinkStyleJpaEntity.builder()
                .id(drinkStyle.getId())
                .source(drinkStyle.getSource())
                .externalId(drinkStyle.getExternalId())
                .name(drinkStyle.getName())
                .category(drinkStyle.getCategory())
                .flavorProfile(FlavorProfileEmbeddable.from(drinkStyle.getFlavorProfile()))
                .build();
    }

    public DrinkStyle toDomain() {
        return DrinkStyle.builder()
                .id(id)
                .source(source)
                .externalId(externalId)
                .name(name)
                .category(category)
                .flavorProfile(flavorProfile.toDomain())
                .build();
    }
}
