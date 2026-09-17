package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.entity.Anju;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnjuJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Embedded private FlavorProfileEmbeddable flavorProfile;

    public static AnjuJpaEntity from(Anju anju) {
        return AnjuJpaEntity.builder()
                .id(anju.getId())
                .name(anju.getName())
                .flavorProfile(FlavorProfileEmbeddable.from(anju.getFlavorProfile()))
                .build();
    }

    public Anju toDomain() {
        return Anju.builder().id(id).name(name).flavorProfile(flavorProfile.toDomain()).build();
    }
}
