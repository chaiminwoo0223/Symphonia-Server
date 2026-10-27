package com.symphonia.catalog.application.dto.result;

import com.symphonia.catalog.domain.entity.Anju;
import com.symphonia.catalog.domain.entity.Drink;
import com.symphonia.catalog.domain.entity.MusicMood;
import com.symphonia.catalog.domain.vo.AllergyType;
import java.util.List;
import java.util.Set;

// 음식 계열(Harmonize) 판정 결과는 계열마다 비트 하나를 둔 harmonizeMask로 담는다.
// 음료와 안주의 harmonizeMask가 한 비트라도 겹치면 둘의 음식 계열이 맞는다.
public record CatalogResult(
        List<DrinkResult> drinks, List<AnjuResult> anjus, List<MusicMoodResult> musicMoods) {
    public CatalogResult {
        drinks = List.copyOf(drinks);
        anjus = List.copyOf(anjus);
        musicMoods = List.copyOf(musicMoods);
    }

    public static CatalogResult of(
            List<Drink> drinks, List<Anju> anjus, List<MusicMood> musicMoods) {
        return new CatalogResult(
                drinks.stream().map(DrinkResult::from).toList(),
                anjus.stream().map(AnjuResult::from).toList(),
                musicMoods.stream().map(MusicMoodResult::from).toList());
    }

    public record DrinkResult(
            Long id,
            Long drinkStyleId,
            String name,
            double abv,
            boolean nonAlcoholic,
            int sweetness,
            int bitterness,
            int carbonation,
            int richness,
            int acidity,
            long harmonizeMask) {
        public static DrinkResult from(Drink drink) {
            return new DrinkResult(
                    drink.getId(),
                    drink.getDrinkStyleId(),
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
                    anju.getHarmonizeMask());
        }
    }

    public record MusicMoodResult(
            Long id,
            String title,
            String streamingUrl,
            int formality,
            int romance,
            int celebration,
            int comfort) {
        public static MusicMoodResult from(MusicMood musicMood) {
            return new MusicMoodResult(
                    musicMood.getId(),
                    musicMood.getTitle(),
                    musicMood.getStreamingUrl(),
                    musicMood.getMoodProfile().getFormality(),
                    musicMood.getMoodProfile().getRomance(),
                    musicMood.getMoodProfile().getCelebration(),
                    musicMood.getMoodProfile().getComfort());
        }
    }
}
