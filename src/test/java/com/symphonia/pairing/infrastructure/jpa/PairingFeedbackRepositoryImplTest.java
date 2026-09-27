package com.symphonia.pairing.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.pairing.domain.entity.PairingFeedback;
import com.symphonia.pairing.domain.repository.PairingFeedbackRepository;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.PairingRating;
import com.symphonia.pairing.domain.vo.RelationshipType;
import com.symphonia.pairing.fixture.AnjuFixture;
import com.symphonia.pairing.fixture.DrinkFixture;
import com.symphonia.pairing.fixture.MusicMoodFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Import(PairingFeedbackRepositoryImpl.class)
@Sql("/sql/clear-pairing-catalog.sql")
class PairingFeedbackRepositoryImplTest extends RepositoryTest {

    @Autowired private PairingFeedbackRepository pairingFeedbackRepository;
    @Autowired private DrinkJpaRepository drinkJpaRepository;
    @Autowired private AnjuJpaRepository anjuJpaRepository;
    @Autowired private MusicMoodJpaRepository musicMoodJpaRepository;

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Test
        @DisplayName("피드백을 저장하고 ID를 채워 반환한다")
        void shouldPersistPairingFeedback() {
            // given
            Long drinkId =
                    drinkJpaRepository
                            .save(DrinkJpaEntity.from(DrinkFixture.SOJU.create()))
                            .getId();
            Long anjuId =
                    anjuJpaRepository
                            .save(AnjuJpaEntity.from(AnjuFixture.GOLBAENGI_MUCHIM.create()))
                            .getId();
            Long musicMoodId =
                    musicMoodJpaRepository
                            .save(MusicMoodJpaEntity.from(MusicMoodFixture.FORMAL_JAZZ.create()))
                            .getId();
            PairingFeedback pairingFeedback =
                    PairingFeedback.of(
                            1L,
                            drinkId,
                            anjuId,
                            musicMoodId,
                            RelationshipType.FRIEND,
                            MoodType.CASUAL,
                            PairingRating.LIKE);

            // when
            PairingFeedback saved = pairingFeedbackRepository.save(pairingFeedback);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getMemberId()).isEqualTo(1L);
            assertThat(saved.getDrinkId()).isEqualTo(drinkId);
            assertThat(saved.getAnjuId()).isEqualTo(anjuId);
            assertThat(saved.getMusicMoodId()).isEqualTo(musicMoodId);
            assertThat(saved.getRelationshipType()).isEqualTo(RelationshipType.FRIEND);
            assertThat(saved.getMoodType()).isEqualTo(MoodType.CASUAL);
            assertThat(saved.getRating()).isEqualTo(PairingRating.LIKE);
        }
    }
}
