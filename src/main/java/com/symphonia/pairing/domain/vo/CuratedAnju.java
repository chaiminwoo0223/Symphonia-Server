package com.symphonia.pairing.domain.vo;

import com.symphonia.pairing.domain.entity.Anju;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

// 사람이 매핑표로 고른 안주다. 맛, 알레르기, 매운맛은 영양성분과 재료 규칙표로 계산하므로 매핑표에 적지 않는다.
// externalId는 행을 식별하는 키이고 kfindCode는 K-FIND 영양성분을 찾는 조회 키다. 시드 행만 두 값이 다르다.
public record CuratedAnju(
        AnjuSource source,
        String externalId,
        String kfindCode,
        String name,
        List<String> mainIngredients,
        List<String> subIngredients,
        AnjuBase base,
        ServingStyle serving) {
    public CuratedAnju {
        mainIngredients = List.copyOf(mainIngredients);
        subIngredients = List.copyOf(subIngredients);
    }

    public List<String> ingredients() {
        return Stream.concat(mainIngredients.stream(), subIngredients.stream()).toList();
    }

    public Anju toAnju(FlavorProfile flavorProfile, Set<AllergyType> allergyTypes, boolean spicy) {
        return Anju.of(source, externalId, name, flavorProfile, allergyTypes, base, serving, spicy);
    }
}
