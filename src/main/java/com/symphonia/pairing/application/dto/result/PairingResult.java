package com.symphonia.pairing.application.dto.result;

import com.symphonia.pairing.domain.vo.Pairing;

public record PairingResult(
        Long drinkId,
        String drinkName,
        boolean drinkNonAlcoholic,
        Long anjuId,
        String anjuName,
        Long musicMoodId,
        String musicMoodTitle,
        String streamingUrl,
        double score) {
    public static PairingResult from(Pairing pairing) {
        return new PairingResult(
                pairing.getDrink().getId(),
                pairing.getDrink().getName(),
                pairing.getDrink().isNonAlcoholic(),
                pairing.getAnju().getId(),
                pairing.getAnju().getName(),
                pairing.getMusicMood().getId(),
                pairing.getMusicMood().getTitle(),
                pairing.getMusicMood().getStreamingUrl(),
                pairing.getScore());
    }
}
