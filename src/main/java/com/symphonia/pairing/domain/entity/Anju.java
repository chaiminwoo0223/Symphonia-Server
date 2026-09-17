package com.symphonia.pairing.domain.entity;

import com.symphonia.pairing.domain.vo.FlavorProfile;
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

    public static Anju of(String name, FlavorProfile flavorProfile) {
        return Anju.builder().name(name).flavorProfile(flavorProfile).build();
    }
}
