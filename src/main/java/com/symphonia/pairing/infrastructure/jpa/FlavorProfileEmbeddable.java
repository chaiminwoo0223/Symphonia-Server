package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.vo.FlavorProfile;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FlavorProfileEmbeddable {
    private int sweetness;
    private int bitterness;
    private int carbonation;
    private int richness;

    public static FlavorProfileEmbeddable from(FlavorProfile flavorProfile) {
        return new FlavorProfileEmbeddable(
                flavorProfile.getSweetness(),
                flavorProfile.getBitterness(),
                flavorProfile.getCarbonation(),
                flavorProfile.getRichness());
    }

    public FlavorProfile toDomain() {
        return FlavorProfile.of(sweetness, bitterness, carbonation, richness);
    }
}
