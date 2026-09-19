package com.symphonia.pairing.domain.vo;

import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.entity.MusicMood;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Pairing {
    private static final double FLAVOR_WEIGHT = 0.6;
    private static final double MOOD_WEIGHT = 0.4;
    private static final double LIGHT_ANJU_PENALTY = 0.1;
    private static final int LIGHT_ANJU_RICHNESS_THRESHOLD = 3;
    private static final double NON_ALCOHOLIC_MISMATCH_PENALTY = 0.2;
    private static final double MODERATE_ABV_THRESHOLD = 20.0;
    private static final double HIGH_ABV_PENALTY = 0.15;
    private static final int RECOMMENDATION_LIMIT = 5;

    private final Drink drink;
    private final Anju anju;
    private final MusicMood musicMood;
    private final double score;

    public static List<Pairing> recommend(
            List<Drink> drinks, List<Anju> anjus, List<MusicMood> musicMoods, Occasion occasion) {
        List<Pairing> pairings = new ArrayList<>();

        for (Drink drink : drinks) {
            if (drink.getAbv() > occasion.maxAbv()) {
                continue;
            }
            for (Anju anju : anjus) {
                for (MusicMood musicMood : musicMoods) {
                    pairings.add(of(drink, anju, musicMood, occasion));
                }
            }
        }

        return pairings.stream()
                .sorted(Comparator.comparingDouble(Pairing::getScore).reversed())
                .limit(RECOMMENDATION_LIMIT)
                .toList();
    }

    private static Pairing of(Drink drink, Anju anju, MusicMood musicMood, Occasion occasion) {
        return new Pairing(drink, anju, musicMood, score(drink, anju, musicMood, occasion));
    }

    private static double score(Drink drink, Anju anju, MusicMood musicMood, Occasion occasion) {
        double flavorSimilarity = drink.getFlavorProfile().similarity(anju.getFlavorProfile());
        double moodFitness = musicMood.getMoodProfile().fitness(occasion.toMoodProfile());
        double score = FLAVOR_WEIGHT * flavorSimilarity + MOOD_WEIGHT * moodFitness;

        if (occasion.prefersLightAnju()
                && anju.getFlavorProfile().getRichness() > LIGHT_ANJU_RICHNESS_THRESHOLD) {
            score -= LIGHT_ANJU_PENALTY;
        }

        if (occasion.requiresNonAlcoholicOption() && !drink.isNonAlcoholic()) {
            score -= NON_ALCOHOLIC_MISMATCH_PENALTY;
        }

        if (drink.getAbv() > MODERATE_ABV_THRESHOLD) {
            score -= HIGH_ABV_PENALTY;
        }

        return Math.max(0, score);
    }
}
