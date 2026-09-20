package com.symphonia.pairing.domain.entity;

import com.symphonia.pairing.domain.vo.AllergyType;
import com.symphonia.pairing.domain.vo.FlavorProfile;
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
    private String name;
    private FlavorProfile flavorProfile;
    private Set<AllergyType> allergyTypes;

    @Builder
    private Anju(Long id, String name, FlavorProfile flavorProfile, Set<AllergyType> allergyTypes) {
        this.id = id;
        this.name = name;
        this.flavorProfile = flavorProfile;
        this.allergyTypes = allergyTypes == null ? Set.of() : Set.copyOf(allergyTypes);
    }

    public static Anju of(String name, FlavorProfile flavorProfile, Set<AllergyType> allergyTypes) {
        return Anju.builder()
                .name(name)
                .flavorProfile(flavorProfile)
                .allergyTypes(allergyTypes)
                .build();
    }

    public boolean conflictsWith(Set<AllergyType> attendeeAllergies) {
        return !Collections.disjoint(allergyTypes, attendeeAllergies);
    }
}
