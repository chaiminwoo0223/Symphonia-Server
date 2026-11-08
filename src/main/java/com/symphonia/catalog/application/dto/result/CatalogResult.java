package com.symphonia.catalog.application.dto.result;

import com.symphonia.catalog.domain.entity.Anju;
import com.symphonia.catalog.domain.entity.Drink;
import com.symphonia.catalog.domain.entity.DrinkStyle;
import com.symphonia.catalog.domain.vo.AllergyType;
import com.symphonia.catalog.domain.vo.AnjuBase;
import com.symphonia.catalog.domain.vo.DrinkCategory;
import com.symphonia.catalog.domain.vo.ServingStyle;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 음식 계열(Harmonize) 판정 결과는 계열마다 비트 하나를 둔 harmonizeMask로 담는다.
// 음료와 안주의 harmonizeMask가 한 비트라도 겹치면 둘의 음식 계열이 맞는다.
// 음료 종류는 음료의 스타일에서 가져온다. 스타일을 찾지 못한 음료는 종류가 null이다.
public record CatalogResult(List<DrinkResult> drinks, List<AnjuResult> anjus) {
    public CatalogResult {
        drinks = List.copyOf(drinks);
        anjus = List.copyOf(anjus);
    }

    public static CatalogResult of(
            List<Drink> drinks, List<DrinkStyle> drinkStyles, List<Anju> anjus) {
        Map<Long, DrinkCategory> categoryByStyleId =
                drinkStyles.stream()
                        .collect(Collectors.toMap(DrinkStyle::getId, DrinkStyle::getCategory));

        return new CatalogResult(
                drinks.stream()
                        .map(
                                drink ->
                                        DrinkResult.from(
                                                drink,
                                                categoryByStyleId.get(drink.getDrinkStyleId())))
                        .toList(),
                anjus.stream().map(AnjuResult::from).toList());
    }

    public record DrinkResult(
            Long id,
            Long drinkStyleId,
            DrinkCategory category,
            String name,
            double abv,
            boolean nonAlcoholic,
            int sweetness,
            int bitterness,
            int carbonation,
            int richness,
            int acidity,
            long harmonizeMask) {
        public static DrinkResult from(Drink drink, DrinkCategory category) {
            return new DrinkResult(
                    drink.getId(),
                    drink.getDrinkStyleId(),
                    category,
                    drink.getName(),
                    drink.getAbv(),
                    drink.isNonAlcoholic(),
                    drink.getFlavorProfile().getSweetness(),
                    drink.getFlavorProfile().getBitterness(),
                    drink.getFlavorProfile().getCarbonation(),
                    drink.getFlavorProfile().getRichness(),
                    drink.getFlavorProfile().getAcidity(),
                    drink.getHarmonizeMask());
        }
    }

    public record AnjuResult(
            Long id,
            String name,
            int sweetness,
            int bitterness,
            int carbonation,
            int richness,
            int acidity,
            Set<AllergyType> allergyTypes,
            AnjuBase base,
            ServingStyle serving,
            boolean spicy,
            long harmonizeMask) {
        public static AnjuResult from(Anju anju) {
            return new AnjuResult(
                    anju.getId(),
                    anju.getName(),
                    anju.getFlavorProfile().getSweetness(),
                    anju.getFlavorProfile().getBitterness(),
                    anju.getFlavorProfile().getCarbonation(),
                    anju.getFlavorProfile().getRichness(),
                    anju.getFlavorProfile().getAcidity(),
                    anju.getAllergyTypes(),
                    anju.getBase(),
                    anju.getServing(),
                    anju.isSpicy(),
                    anju.getHarmonizeMask());
        }
    }
}
