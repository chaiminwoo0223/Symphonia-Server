package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.vo.DrinkSource;
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
@Table(name = "drink")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DrinkJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long drinkStyleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DrinkSource source;

    @Column(nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double abv;

    @Embedded private FlavorProfileEmbeddable flavorProfile;

    @Column(nullable = false)
    private boolean nonAlcoholic;

    public static DrinkJpaEntity from(Drink drink) {
        return DrinkJpaEntity.builder()
                .id(drink.getId())
                .drinkStyleId(drink.getDrinkStyleId())
                .source(drink.getSource())
                .externalId(drink.getExternalId())
                .name(drink.getName())
                .abv(drink.getAbv())
                .flavorProfile(FlavorProfileEmbeddable.from(drink.getFlavorProfile()))
                .nonAlcoholic(drink.isNonAlcoholic())
                .build();
    }

    public Drink toDomain() {
        return Drink.builder()
                .id(id)
                .drinkStyleId(drinkStyleId)
                .source(source)
                .externalId(externalId)
                .name(name)
                .abv(abv)
                .flavorProfile(flavorProfile.toDomain())
                .nonAlcoholic(nonAlcoholic)
                .build();
    }
}
