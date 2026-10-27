package com.symphonia.catalog.helper;

import com.symphonia.catalog.domain.entity.Anju;
import com.symphonia.catalog.domain.entity.Drink;
import com.symphonia.catalog.domain.entity.MusicMood;
import com.symphonia.catalog.fixture.AnjuFixture;
import com.symphonia.catalog.fixture.DrinkFixture;
import com.symphonia.catalog.fixture.MusicMoodFixture;
import com.symphonia.catalog.infrastructure.jpa.AnjuJpaEntity;
import com.symphonia.catalog.infrastructure.jpa.AnjuJpaRepository;
import com.symphonia.catalog.infrastructure.jpa.DrinkJpaEntity;
import com.symphonia.catalog.infrastructure.jpa.DrinkJpaRepository;
import com.symphonia.catalog.infrastructure.jpa.DrinkStyleJpaEntity;
import com.symphonia.catalog.infrastructure.jpa.DrinkStyleJpaRepository;
import com.symphonia.catalog.infrastructure.jpa.MusicMoodJpaEntity;
import com.symphonia.catalog.infrastructure.jpa.MusicMoodJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CatalogHelper {

    private final DrinkStyleJpaRepository drinkStyleJpaRepository;
    private final DrinkJpaRepository drinkJpaRepository;
    private final AnjuJpaRepository anjuJpaRepository;
    private final MusicMoodJpaRepository musicMoodJpaRepository;

    public Drink saveDrink(DrinkFixture drinkFixture) {
        Long drinkStyleId =
                drinkStyleJpaRepository
                        .save(
                                DrinkStyleJpaEntity.from(
                                        drinkFixture.getDrinkStyleFixture().create()))
                        .getId();

        return drinkJpaRepository
                .save(DrinkJpaEntity.from(drinkFixture.createWithStyle(drinkStyleId)))
                .toDomain();
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
