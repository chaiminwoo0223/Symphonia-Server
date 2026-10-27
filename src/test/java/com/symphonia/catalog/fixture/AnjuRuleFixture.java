package com.symphonia.catalog.fixture;

import com.symphonia.catalog.domain.vo.AllergyType;
import com.symphonia.catalog.domain.vo.AnjuRule;
import com.symphonia.catalog.domain.vo.IngredientRule;
import com.symphonia.catalog.domain.vo.NutrientRule;
import com.symphonia.catalog.domain.vo.ServingRichnessRule;
import com.symphonia.catalog.domain.vo.ServingStyle;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 실제 규칙표와 달리 CuratedAnjuFixture의 재료에 걸리는 규칙만 둔 최소 규칙표다.
@Getter
@RequiredArgsConstructor
public enum AnjuRuleFixture {
    STANDARD(
            List.of(new NutrientRule(0, 5, 1), new NutrientRule(5, 999, 4)),
            List.of(new NutrientRule(0, 10, 1), new NutrientRule(10, 999, 4)),
            List.of(new ServingRichnessRule(ServingStyle.SNACK, false, -1)),
            List.of(
                    new IngredientRule("닭", Set.of(AllergyType.CHICKEN), false, 0, 0),
                    new IngredientRule("소고기", Set.of(AllergyType.BEEF), false, 0, 0),
                    new IngredientRule("골뱅이", Set.of(AllergyType.SHELLFISH), false, 0, 0),
                    new IngredientRule(
                            "고추장", Set.of(AllergyType.SOYBEAN, AllergyType.WHEAT), true, 0, 0),
                    new IngredientRule(
                            "간장", Set.of(AllergyType.SOYBEAN, AllergyType.WHEAT), false, 0, 0),
                    new IngredientRule("밀", Set.of(AllergyType.WHEAT), false, 0, 0),
                    new IngredientRule("소면", Set.of(AllergyType.WHEAT), false, 0, 0),
                    new IngredientRule("달걀", Set.of(AllergyType.EGG), false, 0, 0),
                    new IngredientRule("땅콩", Set.of(AllergyType.PEANUT), false, 0, 0),
                    new IngredientRule("고춧가루", Set.of(), true, 0, 0),
                    new IngredientRule("식초", Set.of(), false, 4, 0),
                    new IngredientRule("커피", Set.of(), false, 0, 4),
                    new IngredientRule("마늘", Set.of(), false, 0, 0)));

    private final List<NutrientRule> sugarRules;
    private final List<NutrientRule> fatRules;
    private final List<ServingRichnessRule> servingRichnessRules;
    private final List<IngredientRule> ingredientRules;

    public AnjuRule create() {
        return new AnjuRule(sugarRules, fatRules, servingRichnessRules, ingredientRules);
    }
}
