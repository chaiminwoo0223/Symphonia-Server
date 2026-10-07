package com.symphonia.pairing.domain.vo;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// BJCP 스타일의 IBU, 태그, mouthfeel을 규칙표에 대어 FlavorProfile을 계산한다.
public record FlavorRule(
        List<IbuRule> ibuRules, List<TagRule> tagRules, List<MouthfeelRule> mouthfeelRules) {
    private static final int DEFAULT_VALUE = 1;
    private static final Set<FlavorAxis> DEFAULTABLE_AXES =
            EnumSet.of(FlavorAxis.SWEETNESS, FlavorAxis.ACIDITY);

    public FlavorRule {
        ibuRules = List.copyOf(ibuRules);
        tagRules = List.copyOf(tagRules);
        mouthfeelRules = List.copyOf(mouthfeelRules);
    }

    public FlavorEvaluation evaluate(BjcpStyle style) {
        Map<FlavorAxis, Integer> values = ruleValues(style);
        Map<Boolean, Set<FlavorAxis>> absentAxesByDefaultable = absentAxesByDefaultable(values);

        Set<FlavorAxis> missingAxes = absentAxesByDefaultable.get(false);
        Set<FlavorAxis> defaultedAxes = absentAxesByDefaultable.get(true);
        defaultedAxes.forEach(axis -> values.put(axis, DEFAULT_VALUE));

        return missingAxes.isEmpty()
                ? FlavorEvaluation.determined(style, toFlavorProfile(values), defaultedAxes)
                : FlavorEvaluation.undetermined(style, missingAxes);
    }

    // 같은 축에 여러 출처의 값이 있으면 수치(IBU), 서술(mouthfeel), 분류(태그) 순으로 믿는다.
    private Map<FlavorAxis, Integer> ruleValues(BjcpStyle style) {
        Map<FlavorAxis, Integer> values = new EnumMap<>(FlavorAxis.class);
        values.putAll(tagValues(style));
        values.putAll(mouthfeelValues(style));
        bitterness(style).ifPresent(value -> values.put(FlavorAxis.BITTERNESS, value));
        return values;
    }

    // 규칙이 없는 축을 기본값으로 채울 수 있는 축(true)과 채울 수 없는 축(false)으로 나눈다.
    private Map<Boolean, Set<FlavorAxis>> absentAxesByDefaultable(Map<FlavorAxis, Integer> values) {
        return Arrays.stream(FlavorAxis.values())
                .filter(axis -> !values.containsKey(axis))
                .collect(
                        Collectors.partitioningBy(
                                DEFAULTABLE_AXES::contains,
                                Collectors.toCollection(() -> EnumSet.noneOf(FlavorAxis.class))));
    }

    private Optional<Integer> bitterness(BjcpStyle style) {
        return style.ibu()
                .flatMap(
                        ibu ->
                                ibuRules.stream()
                                        .filter(rule -> rule.contains(ibu))
                                        .map(IbuRule::bitterness)
                                        .findFirst());
    }

    // 태그가 여러 개 걸리면 가장 큰 값을 쓴다.
    private Map<FlavorAxis, Integer> tagValues(BjcpStyle style) {
        return tagRules.stream()
                .filter(rule -> style.tags().contains(rule.tag()))
                .collect(Collectors.toMap(TagRule::axis, TagRule::value, Math::max));
    }

    // mouthfeel은 기본 설명 뒤에 예외를 덧붙이는 경우가 많다. 그래서 가장 먼저 나온 키워드를 쓴다.
    // 같은 위치에서 여러 키워드가 걸리면 더 구체적인 긴 키워드를 쓴다.
    private Map<FlavorAxis, Integer> mouthfeelValues(BjcpStyle style) {
        Comparator<MouthfeelMatch> earliestThenLongest =
                Comparator.comparingInt(MouthfeelMatch::position)
                        .thenComparing(
                                match -> match.rule().keyword().length(),
                                Comparator.reverseOrder());

        return mouthfeelRules.stream()
                .flatMap(
                        rule ->
                                rule.positionIn(style.mouthfeel()).stream()
                                        .mapToObj(position -> new MouthfeelMatch(rule, position)))
                .collect(
                        Collectors.groupingBy(
                                match -> match.rule().axis(),
                                () -> new EnumMap<>(FlavorAxis.class),
                                Collectors.collectingAndThen(
                                        Collectors.minBy(earliestThenLongest),
                                        match -> match.orElseThrow().rule().value())));
    }

    private FlavorProfile toFlavorProfile(Map<FlavorAxis, Integer> values) {
        return FlavorProfile.of(
                values.get(FlavorAxis.SWEETNESS),
                values.get(FlavorAxis.BITTERNESS),
                values.get(FlavorAxis.CARBONATION),
                values.get(FlavorAxis.RICHNESS),
                values.get(FlavorAxis.ACIDITY));
    }

    // mouthfeel에서 키워드가 걸린 규칙과 그 위치를 한 번만 계산해 함께 들고 다닌다.
    private record MouthfeelMatch(MouthfeelRule rule, int position) {}
}
