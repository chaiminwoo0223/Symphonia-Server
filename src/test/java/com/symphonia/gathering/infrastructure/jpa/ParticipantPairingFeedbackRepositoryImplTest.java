package com.symphonia.gathering.infrastructure.jpa;

import static com.symphonia.common.helper.ConcurrencyHelper.await;
import static com.symphonia.common.helper.ConcurrencyHelper.runConcurrently;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.symphonia.RepositoryTest;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.exception.PairingFeedbackAlreadySubmittedException;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantPairingFeedbackRepository;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.infrastructure.crypto.GatheringMasterKey;
import com.symphonia.gathering.infrastructure.crypto.config.GatheringCryptoConfig;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Import({
    ParticipantPairingFeedbackRepositoryImpl.class,
    GatheringRepositoryImpl.class,
    GatheringDataKeyLoader.class,
    GatheringMasterKey.class,
    GatheringCryptoConfig.class
})
class ParticipantPairingFeedbackRepositoryImplTest extends RepositoryTest {

    private static final LocalDateTime SUBMITTED_AT = LocalDateTime.of(2026, 10, 27, 22, 0);

    @Autowired private ParticipantPairingFeedbackRepository participantPairingFeedbackRepository;

    @Autowired
    private ParticipantPairingFeedbackJpaRepository participantPairingFeedbackJpaRepository;

    @Autowired private GatheringRepository gatheringRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long gatheringId;
    private Long participantId;
    private Long leeId;
    private Long otherGatheringId;
    private Long otherParticipantId;

    @BeforeEach
    void setUp() {
        Gathering gathering =
                gatheringRepository.save(
                        GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                List.of(
                                        ParticipantFixture.KIM.create(),
                                        ParticipantFixture.LEE.create())));
        Gathering otherGathering =
                gatheringRepository.save(
                        GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                List.of(ParticipantFixture.KIM.create())));
        List<Long> participantIds =
                gathering.getParticipants().stream().map(Participant::getId).toList();
        gatheringId = gathering.getId();
        participantId = participantIds.get(0);
        leeId = participantIds.get(1);
        otherGatheringId = otherGathering.getId();
        otherParticipantId = otherGathering.getParticipants().getFirst().getId();
    }

    @Nested
    @DisplayName("markSubmitted 메서드는")
    class MarkSubmitted {

        @Nested
        @DisplayName("처음 피드백을 낸 참여자인 경우")
        class WhenNotSubmitted {

            @Test
            @DisplayName("제출 시각과 함께 제출 표시를 저장한다")
            void shouldSaveSubmittedMark() {
                // when
                participantPairingFeedbackRepository.markSubmitted(participantId, SUBMITTED_AT);

                // then
                entityManager.clear();
                assertThat(participantPairingFeedbackJpaRepository.findById(participantId))
                        .map(ParticipantPairingFeedbackJpaEntity::getSubmittedAt)
                        .hasValue(SUBMITTED_AT);
            }
        }

        @Nested
        @DisplayName("같은 참여자의 제출 표시를 다시 남기는 경우")
        class WhenAlreadySubmitted {

            @Test
            @DisplayName("PairingFeedbackAlreadySubmittedException이 발생한다")
            void shouldThrowPairingFeedbackAlreadySubmittedException() {
                // given: 동시 제출로 Service의 중복 검사를 둘 다 통과한 상태
                participantPairingFeedbackRepository.markSubmitted(participantId, SUBMITTED_AT);
                entityManager.clear();

                // when & then
                assertThatThrownBy(
                                () ->
                                        participantPairingFeedbackRepository.markSubmitted(
                                                participantId, SUBMITTED_AT.plusMinutes(1)))
                        .isInstanceOf(PairingFeedbackAlreadySubmittedException.class);
            }
        }

        // 두 트랜잭션이 실제로 동시에 커밋해야 하므로 테스트 트랜잭션 없이 실행하고, 커밋된 행은 테스트가 끝나면 지운다.
        @Nested
        @DisplayName("같은 참여자의 제출 표시를 두 요청이 동시에 남기는 경우")
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        class WhenSubmittedConcurrently {

            @AfterEach
            void tearDown() {
                deleteGatheringRows(gatheringId);
                deleteGatheringRows(otherGatheringId);
            }

            @Test
            @DisplayName("한 요청만 저장되고 다른 요청은 PairingFeedbackAlreadySubmittedException이 발생한다")
            void shouldSaveOnlyOne() {
                // given
                CountDownLatch ready = new CountDownLatch(2);

                // when
                List<Throwable> failures =
                        runConcurrently(
                                () -> markAfterBothReady(ready), () -> markAfterBothReady(ready));

                // then
                assertThat(failures)
                        .singleElement()
                        .isInstanceOf(PairingFeedbackAlreadySubmittedException.class);
                assertThat(
                                jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM participant_pairing_feedback WHERE participant_id = ?",
                                        Long.class,
                                        participantId))
                        .isEqualTo(1L);
            }

            // 두 요청이 모두 Service의 중복 검사를 통과한 뒤 함께 저장하도록 맞춘다.
            private void markAfterBothReady(CountDownLatch ready) {
                transactionTemplate.executeWithoutResult(
                        _ -> {
                            ready.countDown();
                            await(ready);
                            participantPairingFeedbackRepository.markSubmitted(
                                    participantId, SUBMITTED_AT);
                        });
            }
        }
    }

    @Nested
    @DisplayName("existsByParticipantId 메서드는")
    class ExistsByParticipantId {

        @Test
        @DisplayName("피드백을 낸 참여자면 true를 반환한다")
        void shouldReturnTrueWhenSubmitted() {
            // given
            participantPairingFeedbackRepository.markSubmitted(participantId, SUBMITTED_AT);

            // when
            boolean result =
                    participantPairingFeedbackRepository.existsByParticipantId(participantId);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("피드백을 내지 않은 참여자면 false를 반환한다")
        void shouldReturnFalseWhenNotSubmitted() {
            // when
            boolean result =
                    participantPairingFeedbackRepository.existsByParticipantId(participantId);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("deleteAllByGatheringId 메서드는")
    class DeleteAllByGatheringId {

        @Test
        @DisplayName("모임 참여자들의 제출 표시를 모두 지우고 다른 모임 참여자의 제출 표시는 남긴다")
        void shouldDeleteSubmittedMarksOfGatheringOnly() {
            // given
            participantPairingFeedbackRepository.markSubmitted(participantId, SUBMITTED_AT);
            participantPairingFeedbackRepository.markSubmitted(leeId, SUBMITTED_AT);
            participantPairingFeedbackRepository.markSubmitted(otherParticipantId, SUBMITTED_AT);

            // when
            participantPairingFeedbackRepository.deleteAllByGatheringId(gatheringId);

            // then
            assertThat(participantPairingFeedbackRepository.existsByParticipantId(participantId))
                    .isFalse();
            assertThat(participantPairingFeedbackRepository.existsByParticipantId(leeId)).isFalse();
            assertThat(
                            participantPairingFeedbackRepository.existsByParticipantId(
                                    otherParticipantId))
                    .isTrue();
        }
    }

    // 테스트 트랜잭션 없이 커밋한 모임의 행을 외래 키 순서대로 지운다.
    private void deleteGatheringRows(Long gatheringId) {
        jdbcTemplate.update(
                "DELETE FROM participant_pairing_feedback WHERE participant_id IN (SELECT id FROM participant WHERE gathering_id = ?)",
                gatheringId);
        jdbcTemplate.update("DELETE FROM participant WHERE gathering_id = ?", gatheringId);
        jdbcTemplate.update("DELETE FROM gathering_data_key WHERE gathering_id = ?", gatheringId);
        jdbcTemplate.update("DELETE FROM gathering WHERE id = ?", gatheringId);
    }
}
