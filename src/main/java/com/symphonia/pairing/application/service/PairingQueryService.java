package com.symphonia.pairing.application.service;

import com.symphonia.common.annotation.QueryService;
import com.symphonia.pairing.application.dto.query.RecommendPairingQuery;
import com.symphonia.pairing.application.dto.result.PairingResult;
import com.symphonia.pairing.application.usecase.RecommendPairingUseCase;
import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.entity.MusicMood;
import com.symphonia.pairing.domain.repository.AnjuRepository;
import com.symphonia.pairing.domain.repository.DrinkRepository;
import com.symphonia.pairing.domain.repository.MusicMoodRepository;
import com.symphonia.pairing.domain.vo.Occasion;
import com.symphonia.pairing.domain.vo.Pairing;
import java.util.List;
import lombok.RequiredArgsConstructor;

@QueryService
@RequiredArgsConstructor
public class PairingQueryService implements RecommendPairingUseCase {
    private final DrinkRepository drinkRepository;
    private final AnjuRepository anjuRepository;
    private final MusicMoodRepository musicMoodRepository;

    @Override
    public List<PairingResult> recommend(RecommendPairingQuery query) {
        Occasion occasion =
                Occasion.of(
                        query.relationshipType(), query.moodType(), query.attendeeConstraints());

        List<Drink> drinks = drinkRepository.findAll();
        List<Anju> anjus = anjuRepository.findAll();
        List<MusicMood> musicMoods = musicMoodRepository.findAll();

        return Pairing.recommend(drinks, anjus, musicMoods, occasion).stream()
                .map(PairingResult::from)
                .toList();
    }
}
