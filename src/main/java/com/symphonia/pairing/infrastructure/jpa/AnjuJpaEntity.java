package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.vo.AllergyType;
import com.symphonia.pairing.domain.vo.AnjuBase;
import com.symphonia.pairing.domain.vo.AnjuSource;
import com.symphonia.pairing.domain.vo.ServingStyle;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "anju")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnjuJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnjuSource source;

    @Column(nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Embedded private FlavorProfileEmbeddable flavorProfile;

    @ElementCollection
    @CollectionTable(name = "anju_allergy_type", joinColumns = @JoinColumn(name = "anju_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "allergy_type", nullable = false)
    private Set<AllergyType> allergyTypes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnjuBase base;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServingStyle serving;

    @Column(nullable = false)
    private boolean spicy;

    public static AnjuJpaEntity from(Anju anju) {
        return AnjuJpaEntity.builder()
                .id(anju.getId())
                .source(anju.getSource())
                .externalId(anju.getExternalId())
                .name(anju.getName())
                .flavorProfile(FlavorProfileEmbeddable.from(anju.getFlavorProfile()))
                .allergyTypes(anju.getAllergyTypes())
                .base(anju.getBase())
                .serving(anju.getServing())
                .spicy(anju.isSpicy())
                .build();
    }

    public Anju toDomain() {
        return Anju.builder()
                .id(id)
                .source(source)
                .externalId(externalId)
                .name(name)
                .flavorProfile(flavorProfile.toDomain())
                .allergyTypes(allergyTypes)
                .base(base)
                .serving(serving)
                .spicy(spicy)
                .build();
    }
}
