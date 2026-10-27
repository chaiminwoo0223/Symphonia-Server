package com.symphonia.catalog.domain.entity;

import com.symphonia.catalog.domain.vo.AllergyType;
import com.symphonia.catalog.domain.vo.AnjuBase;
import com.symphonia.catalog.domain.vo.AnjuSource;
import com.symphonia.catalog.domain.vo.FlavorProfile;
import com.symphonia.catalog.domain.vo.Harmonize;
import com.symphonia.catalog.domain.vo.ServingStyle;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Anju {
    private Long id;
    private AnjuSource source;
    private String externalId;
    private String name;
    private FlavorProfile flavorProfile;
    private Set<AllergyType> allergyTypes;
    private AnjuBase base;
    private ServingStyle serving;
    private boolean spicy;

    // 맞는 음식 계열은 저장하지 않고 속성으로 정해지므로(#78), 만들 때 한 번 계산해 둔다.
    private long harmonizeMask;

    @Builder
    private Anju(
            Long id,
            AnjuSource source,
            String externalId,
            String name,
            FlavorProfile flavorProfile,
            Set<AllergyType> allergyTypes,
            AnjuBase base,
            ServingStyle serving,
            boolean spicy) {
        this.id = id;
        this.source = source;
        this.externalId = externalId;
        this.name = name;
        this.flavorProfile = flavorProfile;
        this.allergyTypes = allergyTypes == null ? Set.of() : Set.copyOf(allergyTypes);
        this.base = base;
        this.serving = serving;
        this.spicy = spicy;
        this.harmonizeMask = Harmonize.maskMatching(this);
    }

    public static Anju of(
            AnjuSource source,
            String externalId,
            String name,
            FlavorProfile flavorProfile,
            Set<AllergyType> allergyTypes,
            AnjuBase base,
            ServingStyle serving,
            boolean spicy) {
        return Anju.builder()
                .source(source)
                .externalId(externalId)
                .name(name)
                .flavorProfile(flavorProfile)
                .allergyTypes(allergyTypes)
                .base(base)
                .serving(serving)
                .spicy(spicy)
                .build();
    }

    // 다시 적재할 때 매핑표와 규칙표에서 바뀔 수 있는 값만 덮어쓴다. id와 식별 키(source, externalId)는 유지한다.
    public Anju overwriteWith(Anju imported) {
        this.name = imported.name;
        this.flavorProfile = imported.flavorProfile;
        this.allergyTypes = imported.allergyTypes;
        this.base = imported.base;
        this.serving = imported.serving;
        this.spicy = imported.spicy;
        this.harmonizeMask = imported.harmonizeMask;
        return this;
    }
}
