package com.symphonia.catalog.application.dto.result;

import com.symphonia.catalog.domain.entity.Anju;
import com.symphonia.catalog.domain.entity.Drink;
import com.symphonia.catalog.domain.entity.MusicMood;

// id로 가리킨 음료, 안주, 음악 무드를 화면에 보여 줄 값이다.
public record CatalogReferencesResult(
        String drinkName, String anjuName, String musicMoodTitle, String streamingUrl) {
    public static CatalogReferencesResult of(Drink drink, Anju anju, MusicMood musicMood) {
        return new CatalogReferencesResult(
                drink.getName(), anju.getName(), musicMood.getTitle(), musicMood.getStreamingUrl());
    }
}
