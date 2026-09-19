package com.symphonia.pairing.domain.entity;

import com.symphonia.pairing.domain.vo.AllergyType;
import com.symphonia.pairing.domain.vo.FlavorProfile;
import java.util.Collections;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Anju {
    private Long id;
    private String name;
    private FlavorProfile flavorProfile;
    private Set<AllergyType> allergyTypes;

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
