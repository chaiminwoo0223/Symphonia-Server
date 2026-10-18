package com.symphonia.pairing.fixture;

import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.entity.DrinkStyle;
import com.symphonia.pairing.domain.vo.DrinkSource;
import com.symphonia.pairing.domain.vo.FlavorProfile;
import com.symphonia.pairing.domain.vo.XWine;
import java.util.Locale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum XWineFixture {
    ORIGEM_MERLOT("Origem Merlot", "Casa Valduga", "Red", 13.0, "Full-bodied", "Medium"),
    RESERVA_CHARDONNAY("Reserva Chardonnay", "Aurora", "White", 13.0, "Medium-bodied", "High");

    private final String wineName;
    private final String wineryName;
    private final String type;
    private final double abv;
    private final String body;
    private final String acidity;

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
        return new XWine(getWineId(), wineName, wineryName, type, abv, body, acidity);
    }

    // 원본에서 같은 와인의 Body가 바뀌었거나 규칙표에 없는 값이 들어온 행을 만든다.
    public XWine createWithBody(String body) {
        return new XWine(getWineId(), wineName, wineryName, type, abv, body, acidity);
    }

    // FK가 걸린 와인 스타일은 실제로 저장된 id를 받아 아직 저장 전인 와인을 만든다.
    public Drink createDrink(Long drinkStyleId, FlavorProfile flavorProfile) {
        return buildDrink(null, drinkStyleId, flavorProfile);
    }

    // 이미 적재된 와인을 흉내 내야 할 때 id와 당시의 스타일, 맛을 채운다.
    public Drink createImportedDrink(Long id, DrinkStyle drinkStyle, FlavorProfile flavorProfile) {
        return buildDrink(id, drinkStyle.getId(), flavorProfile);
    }

    private Drink buildDrink(Long id, Long drinkStyleId, FlavorProfile flavorProfile) {
        return Drink.builder()
                .id(id)
                .drinkStyleId(drinkStyleId)
                .source(DrinkSource.X_WINES)
                .externalId(getWineId())
                .name(getDisplayName())
                .abv(abv)
                .flavorProfile(flavorProfile)
                .nonAlcoholic(false)
                .build();
    }
}
