package com.symphonia.pairing.domain.vo;

import com.symphonia.pairing.domain.entity.Anju;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// 매핑표의 안주를 영양성분과 재료 규칙표에 대어 맛, 알레르기, 매운맛을 계산한다.
public record AnjuRule(
        List<NutrientRule> sugarRules,
        List<NutrientRule> fatRules,
        List<IngredientRule> ingredientRules) {
    // 안주에는 탄산이 없고, 산미와 쓴맛을 내는 재료가 없으면 그 맛도 없다.
    private static final int ABSENT_TASTE = 0;

    public AnjuRule {
        sugarRules = List.copyOf(sugarRules);
        fatRules = List.copyOf(fatRules);
        ingredientRules = List.copyOf(ingredientRules);
    }

    // 규칙표에 없는 재료가 하나라도 있으면 적재하지 않는다. 그 재료의 알레르기를 놓친 안주가 추천되면 안 되기 때문이다.
    public AnjuEvaluation evaluate(CuratedAnju curatedAnju, Optional<AnjuNutrition> nutrition) {
        Optional<Integer> sweetness =
                nutrition.flatMap(AnjuNutrition::sugar).flatMap(grams -> value(sugarRules, grams));
        Optional<Integer> richness =
                nutrition.flatMap(AnjuNutrition::fat).flatMap(grams -> value(fatRules, grams));
        Set<String> unknownIngredients = unknownIngredients(curatedAnju.ingredients());
        Set<AnjuExclusionReason> exclusionReasons =
                exclusionReasons(curatedAnju, nutrition, sweetness, richness, unknownIngredients);

        if (!exclusionReasons.isEmpty()) {
            return AnjuEvaluation.excluded(curatedAnju, exclusionReasons, unknownIngredients);
        }

        List<IngredientRule> allRules = matchedRules(curatedAnju.ingredients());
        FlavorProfile flavorProfile =
                FlavorProfile.of(
                        sweetness.orElseThrow(),
                        maxTaste(allRules, IngredientRule::bitterness),
                        ABSENT_TASTE,
                        richness.orElseThrow(),
                        maxTaste(allRules, IngredientRule::acidity));
        Anju anju =
                curatedAnju.toAnju(
                        flavorProfile,
                        allergyTypes(allRules, nutrition.orElseThrow().rawMaterials()),
                        isSpicy(curatedAnju.mainIngredients()));
        return AnjuEvaluation.determined(curatedAnju, anju);
    }

    private Set<AnjuExclusionReason> exclusionReasons(
            CuratedAnju curatedAnju,
            Optional<AnjuNutrition> nutrition,
            Optional<Integer> sweetness,
            Optional<Integer> richness,
            Set<String> unknownIngredients) {
        Set<AnjuExclusionReason> reasons = EnumSet.noneOf(AnjuExclusionReason.class);
        if (curatedAnju.externalId().isBlank()) {
            reasons.add(AnjuExclusionReason.MISSING_EXTERNAL_ID);
        }
        if (curatedAnju.kfindCode().isBlank()) {
            reasons.add(AnjuExclusionReason.MISSING_KFIND_CODE);
        } else if (nutrition.isEmpty()) {
            reasons.add(AnjuExclusionReason.NUTRITION_NOT_FOUND);
        } else {
            if (sweetness.isEmpty()) {
                reasons.add(AnjuExclusionReason.SUGAR_NOT_MEASURED);
            }
            if (richness.isEmpty()) {
                reasons.add(AnjuExclusionReason.FAT_NOT_MEASURED);
            }
        }
        if (!unknownIngredients.isEmpty()) {
            reasons.add(AnjuExclusionReason.UNKNOWN_INGREDIENT);
        }
        return reasons;
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

    // 알레르기는 부재료와 가공식품 원재료명까지 모두 본다. 원재료명은 문장이라 재료 단위로 나누지 않고 키워드를 찾는다.
    private Set<AllergyType> allergyTypes(List<IngredientRule> rules, String rawMaterials) {
        Set<AllergyType> allergyTypes = EnumSet.noneOf(AllergyType.class);
        Stream.concat(
                        rules.stream(),
                        ingredientRules.stream().filter(rule -> rule.matches(rawMaterials)))
                .forEach(rule -> allergyTypes.addAll(rule.allergyTypes()));
        return allergyTypes;
    }

    // 매운맛은 주재료에서만 본다. 부재료로 조금 넣은 고춧가루가 안주의 성격을 바꾸면 안 되기 때문이다.
    private boolean isSpicy(List<String> mainIngredients) {
        return matchedRules(mainIngredients).stream().anyMatch(IngredientRule::spicy);
    }
}
