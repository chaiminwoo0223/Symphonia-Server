package com.symphonia.pairing.application.dto.result;

import com.symphonia.pairing.domain.vo.AllergyType;
import com.symphonia.pairing.domain.vo.Pairing;
import com.symphonia.pairing.domain.vo.PairingReason;
import java.util.List;
import java.util.Set;

public record PairingResult(
        Long drinkId,
        String drinkName,
        boolean drinkNonAlcoholic,
        Long anjuId,
        String anjuName,
        Set<AllergyType> anjuAllergyTypes,
        Long musicMoodId,
        String musicMoodTitle,
        String streamingUrl,
        double score,
        List<String> reasons) {
    public static PairingResult from(Pairing pairing) {
        return new PairingResult(
                pairing.getDrink().getId(),
                pairing.getDrink().getName(),
                pairing.getDrink().isNonAlcoholic(),
                pairing.getAnju().getId(),
                pairing.getAnju().getName(),
                pairing.getAnju().getAllergyTypes(),
                pairing.getMusicMood().getId(),
                pairing.getMusicMood().getTitle(),
                pairing.getMusicMood().getStreamingUrl(),
                pairing.getScore(),
                pairing.getReasons().stream().map(PairingReason::message).toList());
    }
}
