package com.symphonia.catalog.domain.vo;

import static java.util.stream.Collectors.toCollection;

import com.symphonia.catalog.domain.entity.DrinkStyle;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// X-Wines 와인의 Type, Body, Acidity를 규칙표에 대어 FlavorProfile을 계산한다.
// Type이 정하는 스타일 기본 맛에서 무게감은 제품의 Body로, 산미는 제품의 Acidity로 덮어쓴다.
// 어울리는 음식 계열은 Harmonize 태그를 규칙표에 대어 정한다.
public record WineFlavorRule(
        List<WineTypeRule> typeRules,
        List<WineBodyRule> bodyRules,
        List<WineAcidityRule> acidityRules,
        List<WineHarmonizeRule> harmonizeRules) {
    public WineFlavorRule {
        typeRules = List.copyOf(typeRules);
        bodyRules = List.copyOf(bodyRules);
        acidityRules = List.copyOf(acidityRules);
        harmonizeRules = List.copyOf(harmonizeRules);
    }

    public List<DrinkStyle> toDrinkStyles() {
        return typeRules.stream().map(WineTypeRule::toDrinkStyle).toList();
    }

    public WineEvaluation evaluate(XWine wine) {
        Optional<WineTypeRule> typeRule =
                typeRules.stream().filter(rule -> rule.matches(wine)).findFirst();
        Optional<WineBodyRule> bodyRule =
                bodyRules.stream().filter(rule -> rule.matches(wine)).findFirst();
        Optional<WineAcidityRule> acidityRule =
                acidityRules.stream().filter(rule -> rule.matches(wine)).findFirst();

        Set<WineAttribute> unknownAttributes =
                unknownAttributes(
                        Map.of(
                                WineAttribute.TYPE, typeRule.isPresent(),
                                WineAttribute.BODY, bodyRule.isPresent(),
                                WineAttribute.ACIDITY, acidityRule.isPresent(),
                                WineAttribute.HARMONIZE, hasRulesForAll(wine.harmonizeTags())));

        return unknownAttributes.isEmpty()
                ? determined(
                        wine,
                        typeRule.orElseThrow(),
                        bodyRule.orElseThrow(),
                        acidityRule.orElseThrow())
                : WineEvaluation.undetermined(wine, unknownAttributes);
    }

    // 규칙표에서 일치하는 행을 찾지 못한 속성만 모은다.
    private Set<WineAttribute> unknownAttributes(Map<WineAttribute, Boolean> matchedByAttribute) {
        return matchedByAttribute.entrySet().stream()
                .filter(entry -> !entry.getValue())
                .map(Map.Entry::getKey)
                .collect(toCollection(() -> EnumSet.noneOf(WineAttribute.class)));
    }

    // 원본에 새 태그가 생기면 그 태그의 음식 계열을 놓치지 않도록 규칙표부터 갱신하게 한다.
    private boolean hasRulesForAll(List<String> wineTags) {
        return wineTags.stream()
                .allMatch(
                        wineTag -> harmonizeRules.stream().anyMatch(rule -> rule.matches(wineTag)));
    }

    private WineEvaluation determined(
            XWine wine, WineTypeRule typeRule, WineBodyRule bodyRule, WineAcidityRule acidityRule) {
        FlavorProfile flavorProfile =
                typeRule.flavorProfile()
                        .withRichness(bodyRule.richness())
                        .withAcidity(acidityRule.value());
        return WineEvaluation.determined(
                wine, typeRule.externalId(), flavorProfile, harmonizesOf(wine.harmonizeTags()));
    }

    private Set<Harmonize> harmonizesOf(List<String> wineTags) {
        return harmonizeRules.stream()
                .filter(rule -> wineTags.stream().anyMatch(rule::matches))
                .flatMap(rule -> rule.harmonizes().stream())
                .collect(toCollection(() -> EnumSet.noneOf(Harmonize.class)));
    }
}
