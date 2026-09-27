package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.vo.FlavorProfile;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FlavorProfileEmbeddable {
    private int sweetness;
    private int bitterness;
    private int carbonation;
    private int richness;

    @ColumnDefault("0")
    private int acidity;

    public static FlavorProfileEmbeddable from(FlavorProfile flavorProfile) {
        return new FlavorProfileEmbeddable(
                flavorProfile.getSweetness(),
                flavorProfile.getBitterness(),
                flavorProfile.getCarbonation(),
                flavorProfile.getRichness(),
                flavorProfile.getAcidity());
    }

    public FlavorProfile toDomain() {
        return FlavorProfile.of(sweetness, bitterness, carbonation, richness, acidity);
    }
}
