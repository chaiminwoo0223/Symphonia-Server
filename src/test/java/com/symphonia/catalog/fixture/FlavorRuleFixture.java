package com.symphonia.catalog.fixture;

import com.symphonia.catalog.domain.vo.FlavorAxis;
import com.symphonia.catalog.domain.vo.FlavorRule;
import com.symphonia.catalog.domain.vo.IbuRule;
import com.symphonia.catalog.domain.vo.MouthfeelRule;
import com.symphonia.catalog.domain.vo.TagRule;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 실제 규칙표와 달리 BjcpStyleFixture의 스타일에 걸리는 규칙만 둔 최소 규칙표다.
@Getter
@RequiredArgsConstructor
public enum FlavorRuleFixture {
    STANDARD(
            List.of(new IbuRule(0, 10, 1), new IbuRule(10, 20, 2)),
            List.of(
                    new TagRule("sweet", FlavorAxis.SWEETNESS, 4),
                    new TagRule("sour", FlavorAxis.ACIDITY, 4)),
            List.of(
                    new MouthfeelRule("moderate carbonation", FlavorAxis.CARBONATION, 3),
                    new MouthfeelRule("medium body", FlavorAxis.RICHNESS, 3)));

    private final List<IbuRule> ibuRules;
    private final List<TagRule> tagRules;
    private final List<MouthfeelRule> mouthfeelRules;

    public FlavorRule create() {
        return new FlavorRule(ibuRules, tagRules, mouthfeelRules);
    }
}
