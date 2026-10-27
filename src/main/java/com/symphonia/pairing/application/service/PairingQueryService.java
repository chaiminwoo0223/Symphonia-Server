package com.symphonia.pairing.application.service;

import com.symphonia.catalog.application.dto.query.CatalogQuery;
import com.symphonia.catalog.application.dto.result.CatalogResult;
import com.symphonia.catalog.application.dto.result.CatalogResult.AnjuResult;
import com.symphonia.catalog.application.dto.result.CatalogResult.DrinkResult;
import com.symphonia.catalog.application.dto.result.CatalogResult.MusicMoodResult;
import com.symphonia.catalog.application.usecase.GetCatalogUseCase;
import com.symphonia.common.annotation.QueryService;
import com.symphonia.pairing.application.dto.query.RecommendPairingQuery;
import com.symphonia.pairing.application.dto.result.PairingResult;
import com.symphonia.pairing.application.usecase.RecommendPairingUseCase;
import com.symphonia.pairing.domain.vo.FlavorProfile;
import com.symphonia.pairing.domain.vo.MoodProfile;
import com.symphonia.pairing.domain.vo.Occasion;
import com.symphonia.pairing.domain.vo.Pairing;
import com.symphonia.pairing.domain.vo.PairingAnju;
import com.symphonia.pairing.domain.vo.PairingDrink;
import com.symphonia.pairing.domain.vo.PairingMusicMood;
import java.util.List;
import lombok.RequiredArgsConstructor;

@QueryService
@RequiredArgsConstructor
public class PairingQueryService implements RecommendPairingUseCase {
    private final GetCatalogUseCase getCatalogUseCase;

    @Override
    public List<PairingResult> recommend(RecommendPairingQuery query) {
        Occasion occasion =
                Occasion.of(
                        query.relationshipType(),
                        query.moodType(),
                        query.attendeeConstraints(),
                        query.attendeeAllergies());

        // 도수 상한을 넘는 음료는 카탈로그 조회 단계에서 먼저 걸러 읽는 행을 줄인다.
        CatalogResult catalog = getCatalogUseCase.get(CatalogQuery.of(occasion.maxAbv()));
        List<PairingDrink> drinks =
                catalog.drinks().stream().map(PairingQueryService::toPairingDrink).toList();
        List<PairingAnju> anjus =
                catalog.anjus().stream().map(PairingQueryService::toPairingAnju).toList();
        List<PairingMusicMood> musicMoods =
                catalog.musicMoods().stream().map(PairingQueryService::toPairingMusicMood).toList();

        return Pairing.recommend(drinks, anjus, musicMoods, occasion).stream()
                .map(PairingResult::from)
                .toList();
    }

    private static PairingDrink toPairingDrink(DrinkResult drink) {
        return PairingDrink.of(
                drink.id(),
                drink.drinkStyleId(),
                drink.name(),
                drink.abv(),
                drink.nonAlcoholic(),
                FlavorProfile.of(
                        drink.sweetness(),
                        drink.bitterness(),
                        drink.carbonation(),
                        drink.richness(),
                        drink.acidity()),
                drink.harmonizeMask());
    }

    private static PairingAnju toPairingAnju(AnjuResult anju) {
        return PairingAnju.of(
                anju.id(),
                anju.name(),
                FlavorProfile.of(
                        anju.sweetness(),
                        anju.bitterness(),
                        anju.carbonation(),
                        anju.richness(),
                        anju.acidity()),
                anju.allergyTypes(),
                anju.harmonizeMask());
    }

    private static PairingMusicMood toPairingMusicMood(MusicMoodResult musicMood) {
        return PairingMusicMood.of(
                musicMood.id(),
                musicMood.title(),
                musicMood.streamingUrl(),
                MoodProfile.of(
                        musicMood.formality(),
                        musicMood.romance(),
                        musicMood.celebration(),
                        musicMood.comfort()));
    }
}
