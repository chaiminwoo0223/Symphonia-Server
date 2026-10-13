package com.symphonia.pairing.domain.vo;

import static java.util.stream.Collectors.toCollection;

import com.symphonia.pairing.domain.entity.DrinkStyle;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// X-Wines 와인의 Type, Body, Acidity를 규칙표에 대어 FlavorProfile을 계산한다.
// Type이 정하는 스타일 기본 맛에서 무게감은 제품의 Body로, 산미는 제품의 Acidity로 덮어쓴다.
public record WineFlavorRule(
        List<WineTypeRule> typeRules,
        List<WineBodyRule> bodyRules,
        List<WineAcidityRule> acidityRules) {
    public WineFlavorRule {
        typeRules = List.copyOf(typeRules);
        bodyRules = List.copyOf(bodyRules);
        acidityRules = List.copyOf(acidityRules);
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
                                WineAttribute.ACIDITY, acidityRule.isPresent()));

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

    private WineEvaluation determined(
            XWine wine, WineTypeRule typeRule, WineBodyRule bodyRule, WineAcidityRule acidityRule) {
        FlavorProfile flavorProfile =
                typeRule.flavorProfile()
                        .withRichness(bodyRule.richness())
                        .withAcidity(acidityRule.value());
        return WineEvaluation.determined(wine, typeRule.externalId(), flavorProfile);
    }
}
