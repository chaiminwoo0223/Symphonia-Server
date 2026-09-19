package com.symphonia.pairing.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.pairing.domain.entity.MusicMood;
import com.symphonia.pairing.domain.repository.MusicMoodRepository;
import com.symphonia.pairing.fixture.MusicMoodFixture;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(MusicMoodRepositoryImpl.class)
class MusicMoodRepositoryImplTest extends RepositoryTest {

    @Autowired private MusicMoodRepository musicMoodRepository;
    @Autowired private MusicMoodJpaRepository musicMoodJpaRepository;

    @Nested
    @DisplayName("findAll 메서드는")
    class FindAll {

        @Test
        @DisplayName("저장된 모든 MusicMood를 반환한다")
        void shouldReturnMusicMoods() {
            // given
            MusicMoodJpaEntity saved =
                    musicMoodJpaRepository.save(
                            MusicMoodJpaEntity.from(MusicMoodFixture.FORMAL_JAZZ.create()));

            // when
            List<MusicMood> result = musicMoodRepository.findAll();

            // then
            assertThat(result).extracting(MusicMood::getId).contains(saved.getId());
        }
    }

    @Nested
    @DisplayName("findById 메서드는")
    class FindById {

        @Test
        @DisplayName("존재하는 ID면 MusicMood를 반환한다")
        void shouldReturnMusicMoodWhenIdExists() {
            // given
            MusicMoodJpaEntity saved =
                    musicMoodJpaRepository.save(
                            MusicMoodJpaEntity.from(MusicMoodFixture.FORMAL_JAZZ.create()));

            // when
            Optional<MusicMood> result = musicMoodRepository.findById(saved.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo(MusicMoodFixture.FORMAL_JAZZ.getTitle());
        }

        @Test
        @DisplayName("존재하지 않는 ID면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenIdNotExists() {
            // when
            Optional<MusicMood> result = musicMoodRepository.findById(-1L);

            // then
            assertThat(result).isEmpty();
        }
    }
}
