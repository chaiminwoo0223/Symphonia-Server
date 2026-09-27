package com.symphonia.pairing.fixture;

import com.symphonia.pairing.domain.vo.BjcpStyle;
import com.symphonia.pairing.domain.vo.IbuRange;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// IBU, mouthfeel 값은 FlavorRuleFixture.STANDARD 기준으로 맛을 정할 수 있는지에 맞춰 골랐다.
@Getter
@RequiredArgsConstructor
public enum BjcpStyleFixture {
    AMERICAN_LIGHT_LAGER(
            "1A",
            "American Light Lager",
            new IbuRange(8.0, 12.0),
            List.of(),
            "medium body, moderate carbonation"),
    BERLINER_WEISSE(
            "23A",
            "Berliner Weisse",
            new IbuRange(3.0, 8.0),
            List.of("sour"),
            "medium body, moderate carbonation"),
    FRUIT_LAMBIC(
            "23F",
            "Fruit Lambic",
            new IbuRange(0.0, 10.0),
            List.of("sweet", "sour"),
            "medium body, moderate carbonation"),
    // IBU와 mouthfeel이 없어 맛을 정할 수 없는 특수 맥주
    COMMERCIAL_SPECIALTY_BEER("34A", "Commercial Specialty Beer", null, List.of(), null);

    private final String styleId;
    private final String name;
    private final IbuRange ibuRange;
    private final List<String> tags;
    private final String mouthfeel;

    public BjcpStyle create() {
        return build(tags);
    }

    public BjcpStyle createWithTags(String... tags) {
        return build(List.of(tags));
    }

    private BjcpStyle build(List<String> tags) {
        return new BjcpStyle(styleId, name, ibuRange, tags, mouthfeel);
    }
}
