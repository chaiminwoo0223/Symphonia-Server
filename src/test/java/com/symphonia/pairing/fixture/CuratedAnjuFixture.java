package com.symphonia.pairing.fixture;

import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.vo.AnjuBase;
import com.symphonia.pairing.domain.vo.AnjuNutrition;
import com.symphonia.pairing.domain.vo.AnjuSource;
import com.symphonia.pairing.domain.vo.CuratedAnju;
import com.symphonia.pairing.domain.vo.FlavorProfile;
import com.symphonia.pairing.domain.vo.ServingStyle;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// K-FIND 행은 식품코드가 식별 키라 externalId와 kfindCode가 같다. 시드 행만 읽을 수 있는 키를 따로 쓴다.
// 다른 Fixture처럼 접두어를 붙여 매핑표와 시드의 키와 겹치지 않게 한다.
@Getter
@RequiredArgsConstructor
public enum CuratedAnjuFixture {
    DAKBAL_BOKKEUM(
            AnjuSource.K_FIND,
            "fixture-D101",
            "fixture-D101",
            "닭발볶음",
            List.of("닭고기", "고춧가루"),
            List.of("고추장", "간장", "마늘"),
            AnjuBase.POULTRY,
            ServingStyle.MAIN,
            3.0,
            12.0,
            ""),
    // 매운 재료(고춧가루)가 부재료에만 있다.
    FRIED_CHICKEN(
            AnjuSource.SEED,
            "fixture-fried-chicken",
            "fixture-D102",
            "후라이드치킨",
            List.of("닭고기"),
            List.of("밀가루", "달걀", "고춧가루"),
            AnjuBase.POULTRY,
            ServingStyle.MAIN,
            1.0,
            15.0,
            ""),
    // 산미를 내는 재료(식초)가 부재료에만 있다.
    GOLBAENGI_MUCHIM(
            AnjuSource.K_FIND,
            "fixture-D103",
            "fixture-D103",
            "골뱅이무침",
            List.of("골뱅이"),
            List.of("소면", "식초"),
            AnjuBase.SEAFOOD,
            ServingStyle.MAIN,
            6.0,
            2.0,
            ""),
    // 가공식품이라 원재료명에 매핑표의 재료에 없는 알레르기(땅콩)가 있다.
    BEEF_JERKY(
            AnjuSource.K_FIND,
            "fixture-P201",
            "fixture-P201",
            "육포",
            List.of("소고기"),
            List.of("간장"),
            AnjuBase.BEEF,
            ServingStyle.SNACK,
            20.0,
            4.0,
            "쇠고기, 간장, 설탕, 땅콩분태");

    private final AnjuSource source;
    private final String externalId;
    private final String kfindCode;
    private final String name;
    private final List<String> mainIngredients;
    private final List<String> subIngredients;
    private final AnjuBase base;
    private final ServingStyle serving;
    private final Double sugarGrams;
    private final Double fatGrams;
    private final String rawMaterials;

    public CuratedAnju create() {
        return build(externalId, kfindCode, subIngredients);
    }

    // 매핑표에서 식별 키나 식품코드를 아직 채우지 않았거나 잘못 채운 행을 만든다.
    public CuratedAnju createWithIdentity(String externalId, String kfindCode) {
        return build(externalId, kfindCode, subIngredients);
    }

    public CuratedAnju createWithSubIngredients(List<String> subIngredients) {
        return build(externalId, kfindCode, subIngredients);
    }

    public AnjuNutrition createNutrition() {
        return createNutrition(sugarGrams, fatGrams);
    }

    public AnjuNutrition createNutrition(Double sugarGrams, Double fatGrams) {
        return new AnjuNutrition(kfindCode, name, sugarGrams, fatGrams, rawMaterials);
    }

    // 이미 적재된 안주를 흉내 내야 할 때 id와 이전 적재 당시의 값을 채운다.
    public Anju createImportedAnju(Long id) {
        return createImportedAnju(id, source);
    }

    // external_id는 같지만 출처가 다른 안주가 이미 적재된 상황을 만든다.
    public Anju createImportedAnju(Long id, AnjuSource source) {
        return Anju.builder()
                .id(id)
                .source(source)
                .externalId(externalId)
                .name("이전 " + name)
                .flavorProfile(FlavorProfile.of(5, 5, 5, 5, 5))
                .allergyTypes(Set.of())
                .base(AnjuBase.SWEET)
                .serving(ServingStyle.SNACK)
                .spicy(true)
                .build();
    }

    private CuratedAnju build(String externalId, String kfindCode, List<String> subIngredients) {
        return new CuratedAnju(
                source,
                externalId,
                kfindCode,
                name,
                mainIngredients,
                subIngredients,
                base,
                serving);
    }
}
