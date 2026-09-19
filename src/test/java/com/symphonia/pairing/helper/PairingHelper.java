package com.symphonia.pairing.helper;

import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.entity.MusicMood;
import com.symphonia.pairing.fixture.AnjuFixture;
import com.symphonia.pairing.fixture.DrinkFixture;
import com.symphonia.pairing.fixture.MusicMoodFixture;
import com.symphonia.pairing.infrastructure.jpa.AnjuJpaEntity;
import com.symphonia.pairing.infrastructure.jpa.AnjuJpaRepository;
import com.symphonia.pairing.infrastructure.jpa.DrinkJpaEntity;
import com.symphonia.pairing.infrastructure.jpa.DrinkJpaRepository;
import com.symphonia.pairing.infrastructure.jpa.MusicMoodJpaEntity;
import com.symphonia.pairing.infrastructure.jpa.MusicMoodJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PairingHelper {

    private final DrinkJpaRepository drinkJpaRepository;
    private final AnjuJpaRepository anjuJpaRepository;
    private final MusicMoodJpaRepository musicMoodJpaRepository;

    public Drink saveDrink(DrinkFixture drinkFixture) {
        return drinkJpaRepository.save(DrinkJpaEntity.from(drinkFixture.create())).toDomain();
    }

    public Anju saveAnju(AnjuFixture anjuFixture) {
        return anjuJpaRepository.save(AnjuJpaEntity.from(anjuFixture.create())).toDomain();
    }

    public MusicMood saveMusicMood(MusicMoodFixture musicMoodFixture) {
        return musicMoodJpaRepository
                .save(MusicMoodJpaEntity.from(musicMoodFixture.create()))
                .toDomain();
    }
}
