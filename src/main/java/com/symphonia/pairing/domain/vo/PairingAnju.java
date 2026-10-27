package com.symphonia.pairing.domain.vo;

import com.symphonia.catalog.domain.vo.AllergyType;
import java.util.Collections;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 추천이 점수를 매기는 안주다. 카탈로그의 안주 중 추천에 필요한 값만 담는다.
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PairingAnju {
    private final Long id;
    private final String name;
    private final FlavorProfile flavorProfile;
    private final Set<AllergyType> allergyTypes;

    @Getter(AccessLevel.PACKAGE)
    private final long harmonizeMask;

    public static PairingAnju of(
            Long id,
            String name,
            FlavorProfile flavorProfile,
            Set<AllergyType> allergyTypes,
            long harmonizeMask) {
        return new PairingAnju(
                id,
                name,
                flavorProfile,
                allergyTypes == null ? Set.of() : Set.copyOf(allergyTypes),
                harmonizeMask);
    }

    public boolean conflictsWith(Set<AllergyType> attendeeAllergies) {
        return !Collections.disjoint(allergyTypes, attendeeAllergies);
    }
}
