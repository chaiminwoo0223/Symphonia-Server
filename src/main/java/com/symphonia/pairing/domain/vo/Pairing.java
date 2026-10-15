package com.symphonia.pairing.domain.vo;

import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.entity.MusicMood;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Pairing {
    private static final double FLAVOR_WEIGHT = 0.6;
    private static final double MOOD_WEIGHT = 0.4;
    private static final double LIGHT_ANJU_PENALTY = 0.1;
    private static final double HIGH_ABV_THRESHOLD = 20.0;
    private static final double HIGH_ABV_PENALTY = 0.15;
    private static final int RECOMMENDATION_LIMIT = 5;
    private static final Comparator<Pairing> RANKING =
            Comparator.comparingDouble((Pairing pairing) -> pairing.rawScore)
                    .reversed()
                    .thenComparing(pairing -> pairing.getDrink().getId())
                    .thenComparing(pairing -> pairing.getAnju().getId())
                    .thenComparing(pairing -> pairing.getMusicMood().getId());

    private final Drink drink;
    private final Anju anju;
    private final MusicMood musicMood;

    // 순위는 감점이 반영된 원점수로 매긴다. 0으로 뭉개면 나쁜 조합끼리의 순서가 id 순으로 바뀐다.
    @Getter(AccessLevel.NONE)
    private final double rawScore;

    private final List<PairingReason> reasons;

    public static List<Pairing> recommend(
            List<Drink> drinks, List<Anju> anjus, List<MusicMood> musicMoods, Occasion occasion) {
        // 음악 점수는 음료, 안주와 무관하므로 음악마다 한 번만 계산한다.
        List<ScoredMusicMood> scoredMusicMoods = scoreMusicMoods(musicMoods, occasion);
        List<Pairing> pairings = new ArrayList<>();

        for (Drink drink : drinks) {
            if (drink.getAbv() > occasion.maxAbv()) {
                continue;
            }
            for (Anju anju : anjus) {
                if (occasion.hasAllergyConflict(anju)) {
                    continue;
                }
                double drinkAnjuScore = drinkAnjuScore(drink, anju, occasion);
                for (ScoredMusicMood scoredMusicMood : scoredMusicMoods) {
                    pairings.add(of(drink, anju, scoredMusicMood, drinkAnjuScore, occasion));
                }
            }
        }

        List<Pairing> ranked = pairings.stream().sorted(RANKING).toList();
        List<Pairing> top = diversifiedTop(ranked);
        List<Pairing> recommended =
                occasion.requiresNonAlcoholicOption() ? withNonAlcoholicOption(top, ranked) : top;

        return recommended.stream().sorted(RANKING).toList();
    }

    // 사용자에게 보여주는 점수는 음수가 되지 않게 0에서 자른다.
    public double getScore() {
        return Math.max(0, rawScore);
    }

    private static List<ScoredMusicMood> scoreMusicMoods(
            List<MusicMood> musicMoods, Occasion occasion) {
        MoodProfile occasionMoodProfile = occasion.toMoodProfile();

        List<ScoredMusicMood> scoredMusicMoods = new ArrayList<>();
        for (MusicMood musicMood : musicMoods) {
            double moodFitness = musicMood.getMoodProfile().fitness(occasionMoodProfile);
            scoredMusicMoods.add(new ScoredMusicMood(musicMood, MOOD_WEIGHT * moodFitness));
        }

        return scoredMusicMoods;
    }

    private static double drinkAnjuScore(Drink drink, Anju anju, Occasion occasion) {
        double flavorSimilarity = drink.getFlavorProfile().similarity(anju.getFlavorProfile());

        return FLAVOR_WEIGHT * flavorSimilarity
                - lightAnjuPenalty(anju, occasion)
                - highAbvPenalty(drink);
    }

    private static double lightAnjuPenalty(Anju anju, Occasion occasion) {
        boolean heavyAnjuDespitePreference =
                occasion.prefersLightAnju() && !anju.getFlavorProfile().isLight();

        return heavyAnjuDespitePreference ? LIGHT_ANJU_PENALTY : 0;
    }

    private static double highAbvPenalty(Drink drink) {
        return drink.getAbv() > HIGH_ABV_THRESHOLD ? HIGH_ABV_PENALTY : 0;
    }

    private static Pairing of(
            Drink drink,
            Anju anju,
            ScoredMusicMood scoredMusicMood,
            double drinkAnjuScore,
            Occasion occasion) {
        MusicMood musicMood = scoredMusicMood.musicMood();
        PairingCandidate candidate = PairingCandidate.of(drink, anju, musicMood, occasion);

        return new Pairing(
                drink,
                anju,
                musicMood,
                drinkAnjuScore + scoredMusicMood.score(),
                PairingReason.allApplicableTo(candidate));
    }

    // 스타일마다 최고점 조합을 하나씩 먼저 고르고, 스타일이 모자라면 남은 조합을 점수순으로 채운다.
    private static List<Pairing> diversifiedTop(List<Pairing> ranked) {
        List<Pairing> picked = new ArrayList<>();
        Set<Long> pickedDrinkStyleIds = new HashSet<>();
        for (Pairing pairing : ranked) {
            if (picked.size() == RECOMMENDATION_LIMIT) {
                break;
            }
            if (pickedDrinkStyleIds.add(pairing.getDrink().getDrinkStyleId())) {
                picked.add(pairing);
            }
        }

        List<Pairing> leftovers =
                ranked.stream()
                        .filter(pairing -> !picked.contains(pairing))
                        .limit(RECOMMENDATION_LIMIT - picked.size())
                        .toList();
        picked.addAll(leftovers);

        return picked;
    }

    private static List<Pairing> withNonAlcoholicOption(List<Pairing> top, List<Pairing> ranked) {
        if (top.stream().anyMatch(pairing -> pairing.getDrink().isNonAlcoholic())) {
            return top;
        }

        return ranked.stream()
                .filter(pairing -> pairing.getDrink().isNonAlcoholic())
                .findFirst()
                .map(bestNonAlcoholic -> replaceLast(top, bestNonAlcoholic))
                .orElse(top);
    }

    private static List<Pairing> replaceLast(List<Pairing> top, Pairing replacement) {
        List<Pairing> replaced = new ArrayList<>(top.subList(0, top.size() - 1));
        replaced.add(replacement);
        return replaced;
    }

    private record ScoredMusicMood(MusicMood musicMood, double score) {}
}
