package com.symphonia.pairing.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.catalog.fixture.AnjuFixture;
import com.symphonia.catalog.fixture.DrinkFixture;
import com.symphonia.catalog.fixture.DrinkStyleFixture;
import com.symphonia.catalog.infrastructure.jpa.AnjuJpaEntity;
import com.symphonia.catalog.infrastructure.jpa.AnjuJpaRepository;
import com.symphonia.catalog.infrastructure.jpa.DrinkJpaEntity;
import com.symphonia.catalog.infrastructure.jpa.DrinkJpaRepository;
import com.symphonia.catalog.infrastructure.jpa.DrinkStyleJpaEntity;
import com.symphonia.catalog.infrastructure.jpa.DrinkStyleJpaRepository;
import com.symphonia.pairing.domain.entity.PairingFeedback;
import com.symphonia.pairing.domain.repository.PairingFeedbackRepository;
import com.symphonia.pairing.domain.vo.CombinationId;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.PairingRating;
import com.symphonia.pairing.domain.vo.RelationshipType;
import com.symphonia.pairing.fixture.PairingFeedbackFixture;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Import(PairingFeedbackRepositoryImpl.class)
@Sql("/sql/clear-catalog.sql")
class PairingFeedbackRepositoryImplTest extends RepositoryTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 99L;

    @Autowired private PairingFeedbackRepository pairingFeedbackRepository;
    @Autowired private PairingFeedbackJpaRepository pairingFeedbackJpaRepository;
    @Autowired private DrinkStyleJpaRepository drinkStyleJpaRepository;
    @Autowired private DrinkJpaRepository drinkJpaRepository;
    @Autowired private AnjuJpaRepository anjuJpaRepository;
    @Autowired private EntityManager entityManager;

    private Long drinkId;
    private Long anjuId;

    // drink는 drink style FK를 가지므로 스타일을 먼저 저장한다.
    @BeforeEach
    void setUp() {
        Long drinkStyleId =
                drinkStyleJpaRepository
                        .save(DrinkStyleJpaEntity.from(DrinkStyleFixture.DISTILLED_SOJU.create()))
                        .getId();
        drinkId =
                drinkJpaRepository
                        .save(DrinkJpaEntity.from(DrinkFixture.SOJU.createWithStyle(drinkStyleId)))
                        .getId();
        anjuId =
                anjuJpaRepository
                        .save(AnjuJpaEntity.from(AnjuFixture.GOLBAENGI_MUCHIM.create()))
                        .getId();
    }

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Nested
        @DisplayName("다시 낸 값으로 바꾼 피드백을 저장하는 경우")
        class WhenResubmitted {

            @Test
            @DisplayName("같은 행의 관계, 분위기, 평점을 바꾼다")
            void shouldUpdatePairingFeedback() {
                // given
                PairingFeedback saved = savedFeedback();
                saved.resubmit(RelationshipType.LOVER, MoodType.ROMANTIC, PairingRating.DISLIKE);

                // when
                pairingFeedbackRepository.save(saved);

                // then
                entityManager.flush();
                entityManager.clear();
                assertThat(
                                pairingFeedbackRepository.findByMemberIdAndCombinationForUpdate(
                                        saved.getMemberId(), drinkId, anjuId))
                        .hasValueSatisfying(
                                feedback -> {
                                    assertThat(feedback.getId()).isEqualTo(saved.getId());
                                    assertThat(feedback.getRelationshipType())
                                            .isEqualTo(RelationshipType.LOVER);
                                    assertThat(feedback.getMoodType()).isEqualTo(MoodType.ROMANTIC);
                                    assertThat(feedback.getRating())
                                            .isEqualTo(PairingRating.DISLIKE);
                                });
            }
        }

        @Nested
        @DisplayName("익명 피드백을 저장하는 경우")
        class WhenAnonymous {

            @Test
            @DisplayName("회원 id 없이 저장한다")
            void shouldSaveFeedbackWithoutMemberId() {
                // given
                PairingFeedback anonymous =
                        PairingFeedbackFixture.FRIEND_FORMAL.createAnonymousWithReferences(
                                drinkId, anjuId);

                // when
                PairingFeedback saved = pairingFeedbackRepository.save(anonymous);

                // then
                entityManager.flush();
                entityManager.clear();
                PairingFeedbackJpaEntity stored =
                        pairingFeedbackJpaRepository.findById(saved.getId()).orElseThrow();
                assertThat(stored.getMemberId()).isNull();
                assertThat(stored.getRating())
                        .isEqualTo(PairingFeedbackFixture.FRIEND_FORMAL.getRating());
            }

            @Test
            @DisplayName("같은 조합에 여러 번 저장해도 회원별 조합 유니크 제약에 걸리지 않고 행마다 새로 쌓인다")
            void shouldAccumulateRowsForSameCombination() {
                // given
                PairingFeedback first =
                        PairingFeedbackFixture.FRIEND_FORMAL.createAnonymousWithReferences(
                                drinkId, anjuId);
                pairingFeedbackRepository.save(first);
                entityManager.flush();
                PairingFeedback second =
                        PairingFeedbackFixture.FRIEND_FORMAL.createAnonymousWithReferences(
                                drinkId, anjuId);

                // when
                pairingFeedbackRepository.save(second);

                // then
                entityManager.flush();
                entityManager.clear();
                List<PairingFeedbackJpaEntity> stored = pairingFeedbackJpaRepository.findAll();
                assertThat(stored)
                        .hasSize(2)
                        .allSatisfy(feedback -> assertThat(feedback.getMemberId()).isNull());
            }
        }
    }

    @Nested
    @DisplayName("saveIfAbsent 메서드는")
    class SaveIfAbsent {

        @Nested
        @DisplayName("회원이 같은 조합에 낸 피드백이 없는 경우")
        class WhenFeedbackNotExists {

            @Test
            @DisplayName("피드백을 생성 시각과 함께 저장하고 true를 반환한다")
            void shouldInsertFeedbackAndReturnTrue() {
                // given
                PairingFeedback pairingFeedback =
                        PairingFeedbackFixture.FRIEND_FORMAL.createWithReferences(drinkId, anjuId);

                // when
                boolean inserted = pairingFeedbackRepository.saveIfAbsent(pairingFeedback);

                // then
                assertThat(inserted).isTrue();
                PairingFeedbackJpaEntity saved = savedEntityOf(pairingFeedback.getMemberId());
                assertThat(saved.getRelationshipType())
                        .isEqualTo(PairingFeedbackFixture.FRIEND_FORMAL.getRelationshipType());
                assertThat(saved.getMoodType())
                        .isEqualTo(PairingFeedbackFixture.FRIEND_FORMAL.getMoodType());
                assertThat(saved.getRating())
                        .isEqualTo(PairingFeedbackFixture.FRIEND_FORMAL.getRating());
                assertThat(saved.getCreatedAt()).isNotNull();
                assertThat(saved.getUpdatedAt()).isNotNull();
            }
        }

        @Nested
        @DisplayName("회원이 같은 조합에 낸 피드백이 있는 경우")
        class WhenFeedbackExists {

            // 동시에 낸 첫 피드백 중 나중 요청도 먼저 넣은 요청이 커밋한 뒤 같은 충돌을 만나므로, 순서대로 두 번 넣어 확인한다.
            @Test
            @DisplayName("기존 피드백을 바꾸지 않고 false를 반환한다")
            void shouldKeepExistingFeedbackAndReturnFalse() {
                // given
                PairingFeedback existing = savedFeedback();
                PairingFeedback another =
                        PairingFeedbackFixture.FRIEND_FORMAL.createWithReferences(drinkId, anjuId);
                another.resubmit(RelationshipType.LOVER, MoodType.ROMANTIC, PairingRating.DISLIKE);

                // when
                boolean inserted = pairingFeedbackRepository.saveIfAbsent(another);

                // then
                assertThat(inserted).isFalse();
                PairingFeedbackJpaEntity saved = savedEntityOf(existing.getMemberId());
                assertThat(saved.getId()).isEqualTo(existing.getId());
                assertThat(saved.getRating()).isEqualTo(existing.getRating());
            }
        }
    }

    @Nested
    @DisplayName("findByMemberIdAndCombinationForUpdate 메서드는")
    class FindByMemberIdAndCombinationForUpdate {

        @Nested
        @DisplayName("회원이 같은 조합에 낸 피드백이 있는 경우")
        class WhenFeedbackExists {

            @Test
            @DisplayName("그 피드백을 반환한다")
            void shouldReturnFeedbackOfMemberAndCombination() {
                // given
                PairingFeedback saved = savedFeedback();

                // when
                Optional<PairingFeedback> found =
                        pairingFeedbackRepository.findByMemberIdAndCombinationForUpdate(
                                saved.getMemberId(), drinkId, anjuId);

                // then
                assertThat(found).map(PairingFeedback::getId).hasValue(saved.getId());
            }
        }

        @Nested
        @DisplayName("같은 조합에 다른 회원의 피드백만 있는 경우")
        class WhenOnlyOtherMemberSubmitted {

            @Test
            @DisplayName("빈 값을 반환한다")
            void shouldReturnEmpty() {
                // given
                savedFeedback();

                // when
                Optional<PairingFeedback> found =
                        pairingFeedbackRepository.findByMemberIdAndCombinationForUpdate(
                                OTHER_MEMBER_ID, drinkId, anjuId);

                // then
                assertThat(found).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("findDislikedCombinationsByMemberId 메서드는")
    class FindDislikedCombinationsByMemberId {

        @Test
        @DisplayName("회원이 싫어요를 남긴 조합만 반환한다")
        void shouldReturnOnlyCombinationsDislikedByMember() {
            // given
            Long likedAnjuId = savedAnjuId(AnjuFixture.DRIED_SNACK);
            Long otherMemberDislikedAnjuId = savedAnjuId(AnjuFixture.TOFU_KIMCHI);
            saveFeedback(MEMBER_ID, drinkId, anjuId, PairingRating.DISLIKE);
            saveFeedback(MEMBER_ID, drinkId, likedAnjuId, PairingRating.LIKE);
            saveFeedback(
                    OTHER_MEMBER_ID, drinkId, otherMemberDislikedAnjuId, PairingRating.DISLIKE);
            pairingFeedbackJpaRepository.save(
                    PairingFeedbackJpaEntity.from(
                            PairingFeedbackFixture.FRIEND_FORMAL.createAnonymousWithReferences(
                                    drinkId, otherMemberDislikedAnjuId)));
            entityManager.flush();
            entityManager.clear();

            // when
            Set<CombinationId> disliked =
                    pairingFeedbackRepository.findDislikedCombinationsByMemberId(MEMBER_ID);

            // then
            assertThat(disliked).containsExactly(new CombinationId(drinkId, anjuId));
        }

        @Test
        @DisplayName("싫어요를 남긴 조합이 없으면 빈 집합을 반환한다")
        void shouldReturnEmptyWhenMemberHasNoDislike() {
            // given
            saveFeedback(MEMBER_ID, drinkId, anjuId, PairingRating.LIKE);
            entityManager.flush();
            entityManager.clear();

            // when
            Set<CombinationId> disliked =
                    pairingFeedbackRepository.findDislikedCombinationsByMemberId(MEMBER_ID);

            // then
            assertThat(disliked).isEmpty();
        }
    }

    private Long savedAnjuId(AnjuFixture anju) {
        return anjuJpaRepository.save(AnjuJpaEntity.from(anju.create())).getId();
    }

    private void saveFeedback(Long memberId, Long drinkId, Long anjuId, PairingRating rating) {
        pairingFeedbackJpaRepository.save(
                PairingFeedbackJpaEntity.from(
                        PairingFeedback.of(
                                memberId, drinkId, anjuId, RelationshipType.FRIEND, null, rating)));
    }

    private PairingFeedback savedFeedback() {
        PairingFeedback pairingFeedback =
                PairingFeedbackFixture.FRIEND_FORMAL.createWithReferences(drinkId, anjuId);
        pairingFeedbackRepository.saveIfAbsent(pairingFeedback);

        return pairingFeedbackRepository
                .findByMemberIdAndCombinationForUpdate(
                        pairingFeedback.getMemberId(), drinkId, anjuId)
                .orElseThrow();
    }

    // 네이티브 INSERT 결과를 영속성 컨텍스트가 아니라 DB에서 읽도록 비운 뒤 조회한다.
    private PairingFeedbackJpaEntity savedEntityOf(Long memberId) {
        entityManager.clear();

        return pairingFeedbackJpaRepository
                .findByMemberIdAndCombinationForUpdate(memberId, drinkId, anjuId)
                .orElseThrow();
    }
}
