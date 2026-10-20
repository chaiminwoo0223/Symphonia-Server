package com.symphonia.pairing.infrastructure.jpa;

import static java.util.stream.Collectors.toCollection;

import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.vo.DrinkSource;
import com.symphonia.pairing.domain.vo.Harmonize;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.EnumSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    // 추천이 요청마다 음료 전체를 읽으므로 컬렉션 테이블 대신 배열 컬럼에 담는다. 값은 Harmonize 이름이다.
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false)
    private String[] harmonizes;

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
                .harmonizes(
                        drink.getHarmonizes().stream()
                                .map(Harmonize::name)
                                .sorted()
                                .toArray(String[]::new))
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
                .harmonizes(
                        Arrays.stream(harmonizes)
                                .map(Harmonize::valueOf)
                                .collect(toCollection(() -> EnumSet.noneOf(Harmonize.class))))
                .build();
    }
}
