package com.symphonia.pairing.domain.vo;

import com.symphonia.pairing.domain.entity.Anju;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// 매핑표의 안주를 영양성분, 형태, 재료 규칙표에 대어 맛, 알레르기, 매운맛을 계산한다.
public record AnjuRule(
        List<NutrientRule> sugarRules,
        List<NutrientRule> fatRules,
        List<ServingRichnessRule> servingRichnessRules,
        List<IngredientRule> ingredientRules) {
    // 안주에는 탄산이 없고, 산미와 쓴맛을 내는 재료가 없으면 그 맛도 없다.
    private static final int ABSENT_TASTE = 0;

    public AnjuRule {
        sugarRules = List.copyOf(sugarRules);
        fatRules = List.copyOf(fatRules);
        servingRichnessRules = List.copyOf(servingRichnessRules);
        ingredientRules = List.copyOf(ingredientRules);
    }

    // 규칙표에 없는 재료가 하나라도 있으면 적재하지 않는다. 그 재료의 알레르기를 놓친 안주가 추천되면 안 되기 때문이다.
    public AnjuEvaluation evaluate(
            CuratedAnju curatedAnju, Map<String, AnjuNutrition> nutritionsByCode) {
        Optional<AnjuNutrition> nutrition =
                Optional.ofNullable(nutritionsByCode.get(curatedAnju.kfindCode()));
        Optional<Integer> sweetness =
                nutrition.flatMap(AnjuNutrition::sugar).flatMap(grams -> value(sugarRules, grams));
        Optional<Integer> richness =
                nutrition.flatMap(AnjuNutrition::fat).flatMap(grams -> value(fatRules, grams));
        Set<String> unknownIngredients = unknownIngredients(curatedAnju.ingredients());
        Set<AnjuExclusionReason> exclusionReasons =
                exclusionReasons(
                        curatedAnju,
                        nutrition.isPresent(),
                        sweetness.isPresent(),
                        richness.isPresent(),
                        unknownIngredients);

        if (!exclusionReasons.isEmpty()) {
            return AnjuEvaluation.excluded(curatedAnju, exclusionReasons, unknownIngredients);
        }

        List<IngredientRule> allRules = matchedRules(curatedAnju.ingredients());
        FlavorProfile flavorProfile =
                FlavorProfile.of(
                        sweetness.orElseThrow(),
                        maxTaste(allRules, IngredientRule::bitterness),
                        ABSENT_TASTE,
                        servingAdjusted(richness.orElseThrow(), curatedAnju.serving()),
                        maxTaste(allRules, IngredientRule::acidity));
        Anju anju =
                curatedAnju.toAnju(
                        flavorProfile,
                        allergyTypes(allRules, nutrition.orElseThrow().rawMaterials()),
                        isSpicy(curatedAnju.mainIngredients()));
        return AnjuEvaluation.determined(curatedAnju, anju);
    }

    private Optional<Integer> value(List<NutrientRule> rules, double grams) {
        return rules.stream()
                .filter(rule -> rule.contains(grams))
                .map(NutrientRule::value)
                .findFirst();
    }

    private Set<String> unknownIngredients(List<String> ingredients) {
        return ingredients.stream()
                .filter(
                        ingredient ->
                                ingredientRules.stream().noneMatch(r -> r.matches(ingredient)))
                .collect(Collectors.toSet());
    }

    private Set<AnjuExclusionReason> exclusionReasons(
            CuratedAnju curatedAnju,
            boolean nutritionPresent,
            boolean sugarMeasured,
            boolean fatMeasured,
            Set<String> unknownIngredients) {
        boolean missingKfindCode = curatedAnju.kfindCode().isBlank();
        boolean nutritionFound = !missingKfindCode && nutritionPresent;
        Map<AnjuExclusionReason, Boolean> violations =
                Map.of(
                        AnjuExclusionReason.MISSING_EXTERNAL_ID,
                        curatedAnju.externalId().isBlank(),
                        AnjuExclusionReason.MISSING_KFIND_CODE,
                        missingKfindCode,
                        AnjuExclusionReason.NUTRITION_NOT_FOUND,
                        !missingKfindCode && !nutritionPresent,
                        AnjuExclusionReason.SUGAR_NOT_MEASURED,
                        nutritionFound && !sugarMeasured,
                        AnjuExclusionReason.FAT_NOT_MEASURED,
                        nutritionFound && !fatMeasured,
                        AnjuExclusionReason.UNKNOWN_INGREDIENT,
                        !unknownIngredients.isEmpty());

        return violations.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(AnjuExclusionReason.class)));
    }

    private List<IngredientRule> matchedRules(Collection<String> ingredients) {
        return ingredients.stream()
                .flatMap(
                        ingredient ->
                                ingredientRules.stream().filter(rule -> rule.matches(ingredient)))
                .toList();
    }

    // 산미와 쓴맛은 식초, 케첩, 커피처럼 소스와 양념인 부재료가 정하는 경우가 많아 재료 전체에서 가장 큰 값을 쓴다.
    private int maxTaste(List<IngredientRule> rules, ToIntFunction<IngredientRule> taste) {
        return rules.stream().mapToInt(taste).max().orElse(ABSENT_TASTE);
    }

    // 형태 보정 규칙이 없는 형태는 지방 구간의 무게감을 그대로 쓴다.
    private int servingAdjusted(int richness, ServingStyle serving) {
        return servingRichnessRules.stream()
                .filter(rule -> rule.matches(serving))
                .findFirst()
                .map(rule -> rule.apply(richness))
                .orElse(richness);
    }

    // 알레르기는 부재료와 가공식품 원재료명까지 모두 본다. 원재료명은 문장이라 재료 단위로 나누지 않고 키워드를 찾는다.
    private Set<AllergyType> allergyTypes(List<IngredientRule> rules, String rawMaterials) {
        return Stream.concat(
                        rules.stream(),
                        ingredientRules.stream().filter(rule -> rule.matches(rawMaterials)))
                .flatMap(rule -> rule.allergyTypes().stream())
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(AllergyType.class)));
    }

    // 매운맛은 주재료에서만 본다. 부재료로 조금 넣은 고춧가루가 안주의 성격을 바꾸면 안 되기 때문이다.
    private boolean isSpicy(List<String> mainIngredients) {
        return matchedRules(mainIngredients).stream().anyMatch(IngredientRule::spicy);
    }
}
