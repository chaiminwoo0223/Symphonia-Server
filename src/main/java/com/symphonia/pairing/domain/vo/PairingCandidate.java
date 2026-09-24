package com.symphonia.pairing.domain.vo;

import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.entity.MusicMood;
import java.util.Objects;

public record PairingCandidate(Drink drink, Anju anju, MusicMood musicMood, Occasion occasion) {
    public PairingCandidate {
        Objects.requireNonNull(drink.getId(), "Drink의 id는 null일 수 없습니다.");
        Objects.requireNonNull(anju.getId(), "Anju의 id는 null일 수 없습니다.");
        Objects.requireNonNull(musicMood.getId(), "MusicMood의 id는 null일 수 없습니다.");
    }

    public static PairingCandidate of(
            Drink drink, Anju anju, MusicMood musicMood, Occasion occasion) {
        return new PairingCandidate(drink, anju, musicMood, occasion);
    }
}
