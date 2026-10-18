package com.symphonia.pairing.domain.vo;

import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.entity.Drink;
import java.util.Arrays;
import java.util.List;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// 허용된 음료×안주 쌍의 점수를 1차원 배열로 들고 있다.
// index번째 쌍은 (index / 안주 수)번째 음료와 (index % 안주 수)번째 안주라서, 한 음료의 쌍은 배열에서 연속한다.
// 쌍마다 점수를 어떻게 매기는지는 모르고, 받은 점수 함수를 그대로 적용한다.
public final class DrinkAnjuScores {
    private final List<Drink> drinks;
    private final List<Anju> anjus;
    private final double[] scores;

    private DrinkAnjuScores(List<Drink> drinks, List<Anju> anjus, double[] scores) {
        this.drinks = drinks;
        this.anjus = anjus;
        this.scores = scores;
    }

    // 요청마다 쌍 수만큼 도는 가장 뜨거운 반복이라 이중 반복문으로 둔다.
    // flatMapToDouble로 바꾸면 음료마다 안쪽 스트림이 생겨 추천 계산 시간이 약 2배가 된다(#107 측정).
    static DrinkAnjuScores of(
            List<Drink> drinks, List<Anju> anjus, ToDoubleBiFunction<Drink, Anju> scoring) {
        double[] scores = new double[drinks.size() * anjus.size()];
        int index = 0;
        for (Drink drink : drinks) {
            for (Anju anju : anjus) {
                scores[index++] = scoring.applyAsDouble(drink, anju);
            }
        }

        return new DrinkAnjuScores(drinks, anjus, scores);
    }

    List<Drink> drinks() {
        return drinks;
    }

    int drinkCount() {
        return drinks.size();
    }

    double[] allScores() {
        return scores;
    }

    // 한 음료와 허용된 안주들의 쌍 중 가장 높은 점수다. 안주가 없으면 음의 무한대다.
    double bestOf(int drinkIndex) {
        int from = drinkIndex * anjus.size();

        return Arrays.stream(scores, from, from + anjus.size())
                .max()
                .orElse(Double.NEGATIVE_INFINITY);
    }

    // 한 음료의 쌍 중 점수가 경계 이상인 쌍을 안주 순서대로 만든다.
    Stream<ScoredDrinkAnju> pairsOf(int drinkIndex, double threshold) {
        Drink drink = drinks.get(drinkIndex);
        int from = drinkIndex * anjus.size();

        return IntStream.range(0, anjus.size())
                .filter(anjuIndex -> scores[from + anjuIndex] >= threshold)
                .mapToObj(
                        anjuIndex ->
                                new ScoredDrinkAnju(
                                        drink, anjus.get(anjuIndex), scores[from + anjuIndex]));
    }
}
