package com.symphonia.pairing.domain.vo;

import com.symphonia.pairing.domain.entity.Anju;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

// 사람이 매핑표로 고른 안주다. 맛, 알레르기, 매운맛은 영양성분과 재료 규칙표로 계산하므로 매핑표에 적지 않는다.
// externalId는 사람이 정한 식별 키이고 kfindCode는 K-FIND 영양성분을 찾는 조회 키다.
// 식품코드는 선정 결과라 고칠 수 있으므로 식별 키로 쓰지 않는다. 그래야 코드를 고쳐도 적재된 안주의 id가 유지된다.
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
