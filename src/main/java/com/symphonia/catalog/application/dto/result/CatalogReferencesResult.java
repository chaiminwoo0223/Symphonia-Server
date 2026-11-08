package com.symphonia.catalog.application.dto.result;

import com.symphonia.catalog.domain.entity.Anju;
import com.symphonia.catalog.domain.entity.Drink;

// id로 가리킨 음료와 안주를 화면에 보여 줄 값이다.
public record CatalogReferencesResult(String drinkName, String anjuName) {
    public static CatalogReferencesResult of(Drink drink, Anju anju) {
        return new CatalogReferencesResult(drink.getName(), anju.getName());
    }
}
