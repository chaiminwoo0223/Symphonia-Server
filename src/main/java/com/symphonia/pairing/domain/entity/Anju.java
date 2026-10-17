package com.symphonia.pairing.domain.entity;

import com.symphonia.pairing.domain.vo.AllergyType;
import com.symphonia.pairing.domain.vo.AnjuBase;
import com.symphonia.pairing.domain.vo.AnjuSource;
import com.symphonia.pairing.domain.vo.FlavorProfile;
import com.symphonia.pairing.domain.vo.ServingStyle;
import java.util.Collections;
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

    public boolean conflictsWith(Set<AllergyType> attendeeAllergies) {
        return !Collections.disjoint(allergyTypes, attendeeAllergies);
    }
}
