package com.symphonia.pairing.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.pairing.fixture.AnjuFixture;
import com.symphonia.pairing.fixture.DrinkFixture;
import com.symphonia.pairing.fixture.MusicMoodFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PairingSeedMigrationTest extends RepositoryTest {

    private static final long SEED_DRINK_COUNT = 8;
    private static final long SEED_ANJU_COUNT = 6;
    private static final long SEED_MUSIC_MOOD_COUNT = 8;
    private static final long SEED_MAX_DRINK_ID = 8;
    private static final long SEED_MAX_ANJU_ID = 6;
    private static final long SEED_MAX_MUSIC_MOOD_ID = 8;

    @Autowired private DrinkJpaRepository drinkJpaRepository;
    @Autowired private AnjuJpaRepository anjuJpaRepository;
    @Autowired private MusicMoodJpaRepository musicMoodJpaRepository;

    @Test
    @DisplayName("V2 시드 마이그레이션은 술, 안주, 음악 무드 시드를 적재한다")
    void shouldLoadSeedCatalog() {
        // given: V2 마이그레이션으로 시드가 적재된 상태

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
    @DisplayName("V2 시드 이후 저장하는 행에는 시드 ID보다 큰 ID를 부여한다")
    void shouldAssignIdGreaterThanSeedIds() {
        // given: V2 마이그레이션이 시드 이후로 IDENTITY 시퀀스를 보정한 상태

        // when
        DrinkJpaEntity drink =
                drinkJpaRepository.save(DrinkJpaEntity.from(DrinkFixture.SOJU.create()));
        AnjuJpaEntity anju =
                anjuJpaRepository.save(AnjuJpaEntity.from(AnjuFixture.BOILED_PORK.create()));
        MusicMoodJpaEntity musicMood =
                musicMoodJpaRepository.save(
                        MusicMoodJpaEntity.from(MusicMoodFixture.CASUAL_LOFI.create()));

        // then
        assertThat(drink.getId()).isGreaterThan(SEED_MAX_DRINK_ID);
        assertThat(anju.getId()).isGreaterThan(SEED_MAX_ANJU_ID);
        assertThat(musicMood.getId()).isGreaterThan(SEED_MAX_MUSIC_MOOD_ID);
    }
}
