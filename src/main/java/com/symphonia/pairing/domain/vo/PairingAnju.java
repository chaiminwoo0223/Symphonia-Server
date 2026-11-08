package com.symphonia.pairing.domain.vo;

import com.symphonia.catalog.domain.vo.AllergyType;
import com.symphonia.catalog.domain.vo.AnjuBase;
import com.symphonia.catalog.domain.vo.ServingStyle;
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
    private final AnjuBase base;
    private final ServingStyle serving;
    private final boolean spicy;

    @Getter(AccessLevel.PACKAGE)
    private final long harmonizeMask;

    public static PairingAnju of(
            Long id,
            String name,
            FlavorProfile flavorProfile,
            Set<AllergyType> allergyTypes,
            AnjuBase base,
            ServingStyle serving,
            boolean spicy,
            long harmonizeMask) {
        return new PairingAnju(
                id,
                name,
                flavorProfile,
                allergyTypes == null ? Set.of() : Set.copyOf(allergyTypes),
                base,
                serving,
                spicy,
                harmonizeMask);
    }

    public boolean conflictsWith(Set<AllergyType> attendeeAllergies) {
        return !Collections.disjoint(allergyTypes, attendeeAllergies);
    }

    public boolean isServedAs(ServingStyle servingStyle) {
        return serving == servingStyle;
    }

    public boolean isBasedOnAnyOf(Set<AnjuBase> bases) {
        return bases.contains(base);
    }
}
