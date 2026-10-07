package com.symphonia.pairing.fixture;

import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.entity.DrinkStyle;
import com.symphonia.pairing.domain.vo.CuratedDrink;
import com.symphonia.pairing.domain.vo.DrinkSource;
import java.util.Locale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CuratedDrinkFixture {
    CASS_FRESH("카스 프레시", 4.5, "2A"),
    HOEGAARDEN("호가든", 4.9, "24A");

    private final String name;
    private final double abv;
    private final String bjcpStyleId;

    // 다른 Fixture처럼 접두어를 붙여, DB 테스트에서 써도 매핑표와 시드의 external_id("cass-fresh" 등)와 겹치지 않게 한다.
    public String getExternalId() {
        return "fixture-" + name().toLowerCase(Locale.ROOT);
    }

    public CuratedDrink create() {
        return new CuratedDrink(getExternalId(), name, abv, bjcpStyleId);
    }

    // 매핑표에서 같은 제품의 이름이나 도수가 바뀐 행을 만든다.
    public CuratedDrink createWithNameAndAbv(String name, double abv) {
        return new CuratedDrink(getExternalId(), name, abv, bjcpStyleId);
    }

    // 이미 적재된 제품을 흉내 내야 할 때 id와 당시 매핑한 스타일을 채운다.
    public Drink createImportedDrink(Long id, DrinkStyle drinkStyle) {
        return Drink.builder()
                .id(id)
                .drinkStyleId(drinkStyle.getId())
                .source(DrinkSource.CURATED)
                .externalId(getExternalId())
                .name(name)
                .abv(abv)
                .flavorProfile(drinkStyle.getFlavorProfile())
                .nonAlcoholic(false)
                .build();
    }
}
