package com.symphonia.pairing.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.catalog.fixture.AnjuFixture;
import com.symphonia.catalog.fixture.DrinkFixture;
import com.symphonia.catalog.fixture.DrinkStyleFixture;
import com.symphonia.catalog.fixture.MusicMoodFixture;
import com.symphonia.catalog.infrastructure.jpa.AnjuJpaEntity;
import com.symphonia.catalog.infrastructure.jpa.AnjuJpaRepository;
import com.symphonia.catalog.infrastructure.jpa.DrinkJpaEntity;
import com.symphonia.catalog.infrastructure.jpa.DrinkJpaRepository;
import com.symphonia.catalog.infrastructure.jpa.DrinkStyleJpaEntity;
import com.symphonia.catalog.infrastructure.jpa.DrinkStyleJpaRepository;
import com.symphonia.catalog.infrastructure.jpa.MusicMoodJpaEntity;
import com.symphonia.catalog.infrastructure.jpa.MusicMoodJpaRepository;
import com.symphonia.pairing.domain.entity.PairingFeedback;
import com.symphonia.pairing.domain.repository.PairingFeedbackRepository;
import com.symphonia.pairing.fixture.PairingFeedbackFixture;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Import(PairingFeedbackRepositoryImpl.class)
@Sql("/sql/clear-catalog.sql")
class PairingFeedbackRepositoryImplTest extends RepositoryTest {

    @Autowired private PairingFeedbackRepository pairingFeedbackRepository;
    @Autowired private DrinkStyleJpaRepository drinkStyleJpaRepository;
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
            // drink는 drink style FK를 가지므로 스타일을 먼저 저장한다.
            Long drinkStyleId =
                    drinkStyleJpaRepository
                            .save(
                                    DrinkStyleJpaEntity.from(
                                            DrinkStyleFixture.DISTILLED_SOJU.create()))
                            .getId();
            Long drinkId =
                    drinkJpaRepository
                            .save(
                                    DrinkJpaEntity.from(
                                            DrinkFixture.SOJU.createWithStyle(drinkStyleId)))
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
                    PairingFeedbackFixture.FRIEND_FORMAL.createWithReferences(
                            drinkId, anjuId, musicMoodId);

            // when
            PairingFeedback saved = pairingFeedbackRepository.save(pairingFeedback);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getMemberId())
                    .isEqualTo(PairingFeedbackFixture.FRIEND_FORMAL.getMemberId());
            assertThat(saved.getDrinkId()).isEqualTo(drinkId);
            assertThat(saved.getAnjuId()).isEqualTo(anjuId);
            assertThat(saved.getMusicMoodId()).isEqualTo(musicMoodId);
            assertThat(saved.getRelationshipType())
                    .isEqualTo(PairingFeedbackFixture.FRIEND_FORMAL.getRelationshipType());
            assertThat(saved.getMoodType())
                    .isEqualTo(PairingFeedbackFixture.FRIEND_FORMAL.getMoodType());
            assertThat(saved.getRating())
                    .isEqualTo(PairingFeedbackFixture.FRIEND_FORMAL.getRating());
        }
    }

    @Nested
    @DisplayName("findReferencedAnjuIds 메서드는")
    class FindReferencedAnjuIds {

        @Test
        @DisplayName("주어진 안주 중 피드백이 참조하는 안주의 id만 반환한다")
        void shouldReturnOnlyReferencedAnjuIds() {
            // given
            Long referencedAnjuId = saveAnju(AnjuFixture.GOLBAENGI_MUCHIM);
            Long unreferencedAnjuId = saveAnju(AnjuFixture.FRIED_CHICKEN);
            saveFeedbackOn(referencedAnjuId);

            // when
            Set<Long> result =
                    pairingFeedbackRepository.findReferencedAnjuIds(
                            List.of(referencedAnjuId, unreferencedAnjuId));

            // then
            assertThat(result).containsExactly(referencedAnjuId);
        }
    }

    private Long saveAnju(AnjuFixture anjuFixture) {
        return anjuJpaRepository.save(AnjuJpaEntity.from(anjuFixture.create())).getId();
    }

    private void saveFeedbackOn(Long anjuId) {
        Long drinkStyleId =
                drinkStyleJpaRepository
                        .save(DrinkStyleJpaEntity.from(DrinkStyleFixture.DISTILLED_SOJU.create()))
                        .getId();
        Long drinkId =
                drinkJpaRepository
                        .save(DrinkJpaEntity.from(DrinkFixture.SOJU.createWithStyle(drinkStyleId)))
                        .getId();
        Long musicMoodId =
                musicMoodJpaRepository
                        .save(MusicMoodJpaEntity.from(MusicMoodFixture.FORMAL_JAZZ.create()))
                        .getId();
        pairingFeedbackRepository.save(
                PairingFeedbackFixture.FRIEND_FORMAL.createWithReferences(
                        drinkId, anjuId, musicMoodId));
    }
}
