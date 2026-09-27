package com.symphonia.pairing.domain.vo;

import com.symphonia.pairing.domain.entity.DrinkStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

// 특수 맥주는 IBU나 mouthfeel이 없을 수 있어 ibuRange는 null을 허용한다.
public record BjcpStyle(
        String styleId, String name, IbuRange ibuRange, List<String> tags, String mouthfeel) {
    public BjcpStyle {
        tags = tags == null ? List.of() : List.copyOf(tags);
        mouthfeel = mouthfeel == null ? "" : mouthfeel.toLowerCase(Locale.ROOT);
    }

    public Optional<Double> ibu() {
        return Optional.ofNullable(ibuRange).map(IbuRange::midpoint);
    }

    // BJCP는 맥주 스타일 가이드라인이므로 출처는 BJCP, 분류는 BEER로 고정된다.
    public DrinkStyle toDrinkStyle(FlavorProfile flavorProfile) {
        return DrinkStyle.of(
                DrinkStyleSource.BJCP, styleId, name, DrinkCategory.BEER, flavorProfile);
    }
}
