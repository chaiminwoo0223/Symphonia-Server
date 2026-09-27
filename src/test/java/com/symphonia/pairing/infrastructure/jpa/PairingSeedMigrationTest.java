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

    @Autowired private DrinkJpaRepository drinkJpaRepository;
    @Autowired private AnjuJpaRepository anjuJpaRepository;
    @Autowired private MusicMoodJpaRepository musicMoodJpaRepository;

    // 시드 마이그레이션에서 setval 보정이 빠지면 IDENTITY가 시드 id를 다시 발급해 save()가 중복 키로 실패한다.
    @Test
    @DisplayName("시드 이후 저장해도 ID가 충돌하지 않는다")
    void shouldSaveWithoutIdCollision() {
        // given: 시드 마이그레이션이 적재된 상태

        // when
        DrinkJpaEntity drink =
                drinkJpaRepository.save(DrinkJpaEntity.from(DrinkFixture.SOJU.create()));
        AnjuJpaEntity anju =
                anjuJpaRepository.save(AnjuJpaEntity.from(AnjuFixture.BOILED_PORK.create()));
        MusicMoodJpaEntity musicMood =
                musicMoodJpaRepository.save(
                        MusicMoodJpaEntity.from(MusicMoodFixture.CASUAL_LOFI.create()));

        // then
        assertThat(drink.getId()).isNotNull();
        assertThat(anju.getId()).isNotNull();
        assertThat(musicMood.getId()).isNotNull();
    }
}
