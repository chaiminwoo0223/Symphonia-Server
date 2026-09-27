package com.symphonia.pairing.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.pairing.fixture.AnjuFixture;
import com.symphonia.pairing.fixture.DrinkFixture;
import com.symphonia.pairing.fixture.MusicMoodFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PairingSeedMigrationTest extends RepositoryTest {

    private static final long SEED_DRINK_COUNT = 8;
    private static final long SEED_ANJU_COUNT = 6;
    private static final long SEED_MUSIC_MOOD_COUNT = 8;

    @Autowired private DrinkJpaRepository drinkJpaRepository;
    @Autowired private AnjuJpaRepository anjuJpaRepository;
    @Autowired private MusicMoodJpaRepository musicMoodJpaRepository;

    @Nested
    @DisplayName("V2 시드 마이그레이션은")
    class SeedPairingData {

        @Test
        @DisplayName("술, 안주, 음악 무드 시드를 적재한다")
        void shouldLoadSeedCatalog() {
            // when
            long drinkCount = drinkJpaRepository.count();
            long anjuCount = anjuJpaRepository.count();
            long musicMoodCount = musicMoodJpaRepository.count();

            // then
            assertThat(drinkCount).isEqualTo(SEED_DRINK_COUNT);
            assertThat(anjuCount).isEqualTo(SEED_ANJU_COUNT);
            assertThat(musicMoodCount).isEqualTo(SEED_MUSIC_MOOD_COUNT);
        }

        @Test
        @DisplayName("시드 이후 저장하는 행에는 시드와 겹치지 않는 ID를 부여한다")
        void shouldAssignIdAfterSeedWhenCatalogSaved() {
            // when
            DrinkJpaEntity drink =
                    drinkJpaRepository.save(DrinkJpaEntity.from(DrinkFixture.SOJU.create()));
            AnjuJpaEntity anju =
                    anjuJpaRepository.save(AnjuJpaEntity.from(AnjuFixture.BOILED_PORK.create()));
            MusicMoodJpaEntity musicMood =
                    musicMoodJpaRepository.save(
                            MusicMoodJpaEntity.from(MusicMoodFixture.CASUAL_LOFI.create()));

            // then
            assertThat(drink.getId()).isGreaterThan(SEED_DRINK_COUNT);
            assertThat(anju.getId()).isGreaterThan(SEED_ANJU_COUNT);
            assertThat(musicMood.getId()).isGreaterThan(SEED_MUSIC_MOOD_COUNT);
        }
    }
}
