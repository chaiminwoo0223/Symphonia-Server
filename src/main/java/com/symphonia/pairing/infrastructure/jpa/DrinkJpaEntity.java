package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.entity.Drink;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DrinkJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double abv;

    @Embedded private FlavorProfileEmbeddable flavorProfile;

    public static DrinkJpaEntity from(Drink drink) {
        return DrinkJpaEntity.builder()
                .id(drink.getId())
                .name(drink.getName())
                .abv(drink.getAbv())
                .flavorProfile(FlavorProfileEmbeddable.from(drink.getFlavorProfile()))
                .build();
    }

    public Drink toDomain() {
        return Drink.builder()
                .id(id)
                .name(name)
                .abv(abv)
                .flavorProfile(flavorProfile.toDomain())
                .build();
    }
}
