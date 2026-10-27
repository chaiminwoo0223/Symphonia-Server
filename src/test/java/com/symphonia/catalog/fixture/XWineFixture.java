package com.symphonia.catalog.fixture;

import com.symphonia.catalog.domain.entity.Drink;
import com.symphonia.catalog.domain.entity.DrinkStyle;
import com.symphonia.catalog.domain.vo.DrinkSource;
import com.symphonia.catalog.domain.vo.FlavorProfile;
import com.symphonia.catalog.domain.vo.Harmonize;
import com.symphonia.catalog.domain.vo.XWine;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum XWineFixture {
    ORIGEM_MERLOT(
            "Origem Merlot",
            "Casa Valduga",
            "Red",
            13.0,
            "Full-bodied",
            "Medium",
            List.of("Beef", "Lamb", "Veal", "Grilled", "Pizza", "Pasta")),
    RESERVA_CHARDONNAY(
            "Reserva Chardonnay",
            "Aurora",
            "White",
            13.0,
            "Medium-bodied",
            "High",
            List.of("Lean Fish", "Shellfish"));

    private final String wineName;
    private final String wineryName;
    private final String type;
    private final double abv;
    private final String body;
    private final String acidity;
    private final List<String> harmonizeTags;

    // 다른 Fixture처럼 접두어를 붙여, DB 테스트에서 써도 X-Wines의 WineID와 겹치지 않게 한다.
    public String getWineId() {
        return "fixture-" + name().toLowerCase(Locale.ROOT);
    }

    public String getDisplayName() {
        return wineryName + " " + wineName;
    }

    public XWine create() {
        return createWithAbv(abv);
    }

    public XWine createWithAbv(double abv) {
        return new XWine(
                getWineId(), wineName, wineryName, type, abv, body, acidity, harmonizeTags);
    }

    // 원본에서 같은 와인의 Body가 바뀌었거나 규칙표에 없는 값이 들어온 행을 만든다.
    public XWine createWithBody(String body) {
        return new XWine(
                getWineId(), wineName, wineryName, type, abv, body, acidity, harmonizeTags);
    }

    // 원본에서 같은 와인의 Harmonize가 바뀌었거나 규칙표에 없는 태그가 들어온 행을 만든다.
    public XWine createWithHarmonizeTags(List<String> harmonizeTags) {
        return new XWine(
                getWineId(), wineName, wineryName, type, abv, body, acidity, harmonizeTags);
    }

    // FK가 걸린 와인 스타일은 실제로 저장된 id를 받아 아직 저장 전인 와인을 만든다.
    public Drink createDrink(Long drinkStyleId, FlavorProfile flavorProfile) {
        return buildDrink(null, drinkStyleId, flavorProfile);
    }

    // 이미 적재된 와인을 흉내 내야 할 때 id와 당시의 스타일, 맛, 음식 계열을 채운다.
    public Drink createImportedDrink(
            Long id,
            DrinkStyle drinkStyle,
            FlavorProfile flavorProfile,
            Set<Harmonize> harmonizes) {
        return buildDrink(id, drinkStyle.getId(), flavorProfile, harmonizes);
    }

    private Drink buildDrink(Long id, Long drinkStyleId, FlavorProfile flavorProfile) {
        return buildDrink(id, drinkStyleId, flavorProfile, Set.of());
    }

    private Drink buildDrink(
            Long id, Long drinkStyleId, FlavorProfile flavorProfile, Set<Harmonize> harmonizes) {
        return Drink.builder()
                .id(id)
                .drinkStyleId(drinkStyleId)
                .source(DrinkSource.X_WINES)
                .externalId(getWineId())
                .name(getDisplayName())
                .abv(abv)
                .flavorProfile(flavorProfile)
                .nonAlcoholic(false)
                .harmonizes(harmonizes)
                .build();
    }
}
