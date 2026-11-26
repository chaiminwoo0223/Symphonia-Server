package com.symphonia.catalog.domain.entity;

import com.symphonia.catalog.domain.vo.AllergyType;
import com.symphonia.catalog.domain.vo.DrinkSource;
import com.symphonia.catalog.domain.vo.FlavorProfile;
import com.symphonia.catalog.domain.vo.Harmonize;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Drink {
    private Long id;
    private Long drinkStyleId;
    private DrinkSource source;
    private String externalId;
    private String name;
    private double abv;
    private FlavorProfile flavorProfile;
    private boolean nonAlcoholic;
    private Set<Harmonize> harmonizes;
    private long harmonizeMask;
    private Set<AllergyType> allergyTypes;

    @Builder
    private Drink(
            Long id,
            Long drinkStyleId,
            DrinkSource source,
            String externalId,
            String name,
            double abv,
            FlavorProfile flavorProfile,
            boolean nonAlcoholic,
            Set<Harmonize> harmonizes,
            Set<AllergyType> allergyTypes) {
        this.id = id;
        this.drinkStyleId = drinkStyleId;
        this.source = source;
        this.externalId = externalId;
        this.name = name;
        this.abv = abv;
        this.flavorProfile = flavorProfile;
        this.nonAlcoholic = nonAlcoholic;
        this.harmonizes = harmonizes == null ? Set.of() : Set.copyOf(harmonizes);
        this.harmonizeMask = Harmonize.maskOf(this.harmonizes);
        this.allergyTypes = allergyTypes == null ? Set.of() : Set.copyOf(allergyTypes);
    }

    public static Drink of(
            Long drinkStyleId,
            DrinkSource source,
            String externalId,
            String name,
            double abv,
            FlavorProfile flavorProfile,
            boolean nonAlcoholic,
            Set<Harmonize> harmonizes,
            Set<AllergyType> allergyTypes) {
        return Drink.builder()
                .drinkStyleId(drinkStyleId)
                .source(source)
                .externalId(externalId)
                .name(name)
                .abv(abv)
                .flavorProfile(flavorProfile)
                .nonAlcoholic(nonAlcoholic)
                .harmonizes(harmonizes)
                .allergyTypes(allergyTypes)
                .build();
    }

    // 다시 적재할 때 원본에서 바뀔 수 있는 값만 덮어쓴다. id와 식별 키(source, externalId), 무알코올 여부는 유지한다.
    public Drink overwriteWith(Drink imported) {
        this.drinkStyleId = imported.drinkStyleId;
        this.name = imported.name;
        this.abv = imported.abv;
        this.flavorProfile = imported.flavorProfile;
        this.harmonizes = imported.harmonizes;
        this.harmonizeMask = imported.harmonizeMask;
        this.allergyTypes = imported.allergyTypes;
        return this;
    }
}
