package com.symphonia.gathering.infrastructure.jpa;

import static com.symphonia.common.helper.ConcurrencyHelper.await;
import static com.symphonia.common.helper.ConcurrencyHelper.runConcurrently;
import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantPairingFeedbackRepository;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.infrastructure.crypto.GatheringMasterKey;
import com.symphonia.gathering.infrastructure.crypto.config.GatheringCryptoConfig;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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
    @DisplayName("markSubmittedIfAbsent 메서드는")
    class MarkSubmittedIfAbsent {

        @Nested
        @DisplayName("처음 피드백을 낸 참여자인 경우")
        class WhenNotSubmitted {

            @Test
            @DisplayName("제출 시각과 함께 제출 표시를 저장하고 true를 반환한다")
            void shouldSaveSubmittedMarkAndReturnTrue() {
                // when
                boolean marked =
                        participantPairingFeedbackRepository.markSubmittedIfAbsent(
                                participantId, SUBMITTED_AT);

                // then
                assertThat(marked).isTrue();
                entityManager.clear();
                assertThat(participantPairingFeedbackJpaRepository.findById(participantId))
                        .map(ParticipantPairingFeedbackJpaEntity::getSubmittedAt)
                        .hasValue(SUBMITTED_AT);
            }
        }

        @Nested
        @DisplayName("같은 참여자의 제출 표시가 이미 있는 경우")
        class WhenAlreadySubmitted {

            @Test
            @DisplayName("기존 제출 시각을 바꾸지 않고 false를 반환한다")
            void shouldKeepSubmittedMarkAndReturnFalse() {
                // given
                participantPairingFeedbackRepository.markSubmittedIfAbsent(
                        participantId, SUBMITTED_AT);

                // when
                boolean marked =
                        participantPairingFeedbackRepository.markSubmittedIfAbsent(
                                participantId, SUBMITTED_AT.plusMinutes(1));

                // then
                assertThat(marked).isFalse();
                entityManager.clear();
                assertThat(participantPairingFeedbackJpaRepository.findById(participantId))
                        .map(ParticipantPairingFeedbackJpaEntity::getSubmittedAt)
                        .hasValue(SUBMITTED_AT);
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
            @DisplayName("예외 없이 한 요청만 저장해 true를 반환하고 다른 요청은 false를 반환한다")
            void shouldSaveOnlyOne() {
                // given
                CountDownLatch ready = new CountDownLatch(2);
                Queue<Boolean> results = new ConcurrentLinkedQueue<>();

                // when
                List<Throwable> failures =
                        runConcurrently(
                                () -> markAfterBothReady(ready, results),
                                () -> markAfterBothReady(ready, results));

                // then
                assertThat(failures).isEmpty();
                assertThat(results).containsExactlyInAnyOrder(true, false);
                assertThat(
                                jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM participant_pairing_feedback WHERE participant_id = ?",
                                        Long.class,
                                        participantId))
                        .isEqualTo(1L);
            }

            // 두 요청이 같은 시점에 표시를 넣도록 맞춘다.
            private void markAfterBothReady(CountDownLatch ready, Queue<Boolean> results) {
                transactionTemplate.executeWithoutResult(
                        _ -> {
                            ready.countDown();
                            await(ready);
                            results.add(
                                    participantPairingFeedbackRepository.markSubmittedIfAbsent(
                                            participantId, SUBMITTED_AT));
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
            participantPairingFeedbackRepository.markSubmittedIfAbsent(participantId, SUBMITTED_AT);

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
            participantPairingFeedbackRepository.markSubmittedIfAbsent(participantId, SUBMITTED_AT);
            participantPairingFeedbackRepository.markSubmittedIfAbsent(leeId, SUBMITTED_AT);
            participantPairingFeedbackRepository.markSubmittedIfAbsent(
                    otherParticipantId, SUBMITTED_AT);

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
