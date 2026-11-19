package com.symphonia.gathering.infrastructure.jpa;

import static com.symphonia.common.helper.ConcurrencyHelper.await;
import static com.symphonia.common.helper.ConcurrencyHelper.runConcurrently;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.symphonia.RepositoryTest;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.GatheringRound;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.exception.ParticipantAlreadyJoinedException;
import com.symphonia.gathering.domain.exception.ParticipantUnremovableException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.PhoneNumber;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.GatheringRoundFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import com.symphonia.gathering.infrastructure.crypto.GatheringMasterKey;
import com.symphonia.gathering.infrastructure.crypto.config.GatheringCryptoConfig;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.AfterEach;
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
    GatheringRepositoryImpl.class,
    SettlementRepositoryImpl.class,
    GatheringDataKeyLoader.class,
    GatheringMasterKey.class,
    GatheringCryptoConfig.class
})
class GatheringRepositoryImplTest extends RepositoryTest {

    @Autowired private GatheringRepository gatheringRepository;
    @Autowired private SettlementRepository settlementRepository;
    @Autowired private ParticipantJpaRepository participantJpaRepository;
    @Autowired private GatheringDataKeyJpaRepository gatheringDataKeyJpaRepository;
    @Autowired private GatheringRoundJpaRepository gatheringRoundJpaRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Nested
        @DisplayName("처음 저장하는 모임인 경우")
        class WhenGatheringIsNew {

            @Test
            @DisplayName("모임과 함께 모임별 데이터 키를 저장한다")
            void shouldPersistGatheringWithDataKey() {
                // given
                Gathering gathering = GatheringFixture.FRIDAY_DINNER.create();

                // when
                Gathering saved = gatheringRepository.save(gathering);

                // then
                assertThat(saved.getId()).isNotNull();
                assertThat(gatheringDataKeyJpaRepository.findById(saved.getId())).isPresent();
            }

            @Test
            @DisplayName("1차를 저장하고 id가 채워진 1차를 담아 반환한다")
            void shouldPersistFirstRound() {
                // given
                Gathering gathering = GatheringFixture.FRIDAY_DINNER.create();

                // when
                Gathering saved = gatheringRepository.save(gathering);

                // then
                assertThat(saved.getRounds())
                        .singleElement()
                        .satisfies(
                                round -> {
                                    assertThat(round.getId()).isNotNull();
                                    assertThat(round.getNumber())
                                            .isEqualTo(GatheringRoundFixture.FIRST.getNumber());
                                });
                clearPersistenceContext();
                assertThat(gatheringRepository.findById(saved.getId()).orElseThrow().getRounds())
                        .extracting(GatheringRound::getId, GatheringRound::getNumber)
                        .containsExactly(
                                tuple(
                                        saved.getRounds().getFirst().getId(),
                                        GatheringRoundFixture.FIRST.getNumber()));
            }

            @Test
            @DisplayName("모임을 연 시각을 저장한다")
            void shouldPersistOpenedAt() {
                // given
                Gathering gathering = GatheringFixture.FRIDAY_DINNER.create();

                // when
                Gathering saved = gatheringRepository.save(gathering);

                // then
                clearPersistenceContext();
                assertThat(gatheringRepository.findById(saved.getId()).orElseThrow().getOpenedAt())
                        .isEqualTo(gathering.getOpenedAt());
            }

            @Test
            @DisplayName("참여자의 이름과 휴대폰을 평문 없이 암호화해 저장한다")
            void shouldEncryptParticipantNameAndPhoneNumber() {
                // given
                Gathering gathering =
                        GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                List.of(ParticipantFixture.KIM.create()));

                // when
                Gathering saved = gatheringRepository.save(gathering);

                // then
                ParticipantJpaEntity stored =
                        participantJpaRepository
                                .findAllByGatheringIdOrderById(saved.getId())
                                .getFirst();
                assertThat(new String(stored.getEncryptedName(), StandardCharsets.UTF_8))
                        .doesNotContain(ParticipantFixture.KIM.getName());
                assertThat(new String(stored.getEncryptedPhoneNumber(), StandardCharsets.UTF_8))
                        .doesNotContain(ParticipantFixture.KIM.getPhoneNumber());
                assertThat(stored.getPhoneNumberHmac())
                        .doesNotContain(ParticipantFixture.KIM.getPhoneNumber());
            }
        }

        @Nested
        @DisplayName("이미 저장된 모임에 참여자를 더한 경우")
        class WhenParticipantJoinsSavedGathering {

            @Test
            @DisplayName("기존 참여자를 유지하고 새 참여자를 함께 저장한다")
            void shouldPersistJoinedParticipant() {
                // given
                Gathering saved =
                        gatheringRepository.save(
                                GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                        List.of(ParticipantFixture.KIM.create())));
                clearPersistenceContext();
                Gathering loaded = gatheringRepository.findById(saved.getId()).orElseThrow();
                joinLee(loaded);

                // when
                gatheringRepository.save(loaded);

                // then
                clearPersistenceContext();
                assertThat(
                                gatheringRepository
                                        .findById(saved.getId())
                                        .orElseThrow()
                                        .getParticipants())
                        .extracting(Participant::getName)
                        .containsExactly(
                                ParticipantFixture.KIM.getName(), ParticipantFixture.LEE.getName());
            }

            @Test
            @DisplayName("기존 참여자의 행은 다시 쓰지 않는다")
            void shouldNotRewriteExistingParticipant() {
                // given: 다시 암호화하면 IV가 바뀌어 암호문이 달라지는 상태
                Gathering saved =
                        gatheringRepository.save(
                                GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                        List.of(ParticipantFixture.KIM.create())));
                clearPersistenceContext();
                byte[] encryptedNameBeforeJoin =
                        participantJpaRepository
                                .findAllByGatheringIdOrderById(saved.getId())
                                .getFirst()
                                .getEncryptedName();
                Gathering loaded = gatheringRepository.findById(saved.getId()).orElseThrow();
                joinLee(loaded);

                // when
                gatheringRepository.save(loaded);

                // then
                clearPersistenceContext();
                assertThat(
                                participantJpaRepository
                                        .findAllByGatheringIdOrderById(saved.getId())
                                        .getFirst()
                                        .getEncryptedName())
                        .isEqualTo(encryptedNameBeforeJoin);
            }

            @Test
            @DisplayName("기존 참여자 다음에 id가 채워진 새 참여자를 담아 반환한다")
            void shouldReturnExistingParticipantsBeforeJoinedParticipant() {
                // given
                Gathering saved =
                        gatheringRepository.save(
                                GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                        List.of(ParticipantFixture.KIM.create())));
                Long existingParticipantId = saved.getParticipants().getFirst().getId();
                clearPersistenceContext();
                Gathering loaded = gatheringRepository.findById(saved.getId()).orElseThrow();
                joinLee(loaded);

                // when
                Gathering result = gatheringRepository.save(loaded);

                // then
                List<Participant> participants = result.getParticipants();
                assertThat(participants)
                        .extracting(Participant::getName)
                        .containsExactly(
                                ParticipantFixture.KIM.getName(), ParticipantFixture.LEE.getName());
                assertThat(participants.getFirst().getId()).isEqualTo(existingParticipantId);
                assertThat(participants.getLast().getId())
                        .isNotNull()
                        .isNotEqualTo(existingParticipantId);
            }

            private void joinLee(Gathering gathering) {
                gathering.join(
                        ParticipantFixture.LEE.getName(),
                        new PhoneNumber(ParticipantFixture.LEE.getPhoneNumber()),
                        GatheringPolicy.CONSENT_VERSION,
                        gathering.getOpenedAt().plusHours(1));
            }
        }

        @Nested
        @DisplayName("이미 저장된 모임에 차수를 더한 경우")
        class WhenRoundAddedToSavedGathering {

            @Test
            @DisplayName("기존 차수의 id를 유지하고 새 차수만 id를 채워 반환한다")
            void shouldKeepExistingRoundAndAddNewRound() {
                // given
                Gathering saved = gatheringRepository.save(GatheringFixture.FRIDAY_DINNER.create());
                Long firstRoundId = saved.getRounds().getFirst().getId();
                clearPersistenceContext();
                Gathering loaded = gatheringRepository.findById(saved.getId()).orElseThrow();
                loaded.addRound(GatheringRoundFixture.SECOND.getNumber());

                // when
                Gathering result = gatheringRepository.save(loaded);

                // then
                assertThat(result.getRounds())
                        .extracting(GatheringRound::getNumber)
                        .containsExactly(1, 2);
                assertThat(result.getRounds().getFirst().getId()).isEqualTo(firstRoundId);
                assertThat(result.getRounds().getLast().getId())
                        .isNotNull()
                        .isNotEqualTo(firstRoundId);
                clearPersistenceContext();
                assertThat(
                                gatheringRoundJpaRepository.findAllByGatheringIdOrderByRoundNumber(
                                        saved.getId()))
                        .hasSize(2);
            }
        }

        @Nested
        @DisplayName("같은 번호의 차수를 두 번 더해 저장한 경우")
        class WhenSameRoundNumberSavedTwice {

            @Test
            @DisplayName("행을 하나만 두고 예외 없이 먼저 저장한 차수를 반환한다")
            void shouldKeepSingleRowAndReturnExistingRound() {
                // given: 두 요청이 같은 모임을 읽어 각자 2차를 더한 상태에서 한 요청이 먼저 저장한 상태
                Gathering saved = gatheringRepository.save(GatheringFixture.FRIDAY_DINNER.create());
                clearPersistenceContext();
                Gathering first = gatheringRepository.findById(saved.getId()).orElseThrow();
                Gathering second = gatheringRepository.findById(saved.getId()).orElseThrow();
                first.addRound(GatheringRoundFixture.SECOND.getNumber());
                second.addRound(GatheringRoundFixture.SECOND.getNumber());
                Long firstSavedRoundId =
                        gatheringRepository.save(first).getRounds().getLast().getId();
                clearPersistenceContext();

                // when
                Gathering result = gatheringRepository.save(second);

                // then
                assertThat(result.getRounds())
                        .extracting(GatheringRound::getNumber)
                        .containsExactly(1, 2);
                assertThat(result.getRounds().getLast().getId()).isEqualTo(firstSavedRoundId);
                clearPersistenceContext();
                assertThat(
                                gatheringRoundJpaRepository.findAllByGatheringIdOrderByRoundNumber(
                                        saved.getId()))
                        .hasSize(2);
            }
        }

        // 두 트랜잭션이 실제로 동시에 커밋해야 하므로 테스트 트랜잭션 없이 실행하고, 커밋된 행은 테스트가 끝나면 지운다.
        @Nested
        @DisplayName("두 요청이 같은 번호의 차수를 동시에 더해 저장하는 경우")
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        class WhenSameRoundAddedConcurrently {

            private Long gatheringId;

            @AfterEach
            void tearDown() {
                jdbcTemplate.update(
                        "DELETE FROM gathering_round WHERE gathering_id = ?", gatheringId);
                jdbcTemplate.update(
                        "DELETE FROM gathering_data_key WHERE gathering_id = ?", gatheringId);
                jdbcTemplate.update("DELETE FROM gathering WHERE id = ?", gatheringId);
            }

            @Test
            @DisplayName("행을 하나만 두고 두 요청 모두 예외 없이 같은 차수를 반환받는다")
            void shouldKeepSingleRowAndReturnSameRound() {
                // given
                gatheringId =
                        transactionTemplate.execute(
                                _ ->
                                        gatheringRepository
                                                .save(GatheringFixture.FRIDAY_DINNER.create())
                                                .getId());
                CountDownLatch loaded = new CountDownLatch(2);
                List<Long> savedRoundIds = new CopyOnWriteArrayList<>();

                // when
                List<Throwable> failures =
                        runConcurrently(
                                () -> savedRoundIds.add(addSecondRoundAfterBothLoaded(loaded)),
                                () -> savedRoundIds.add(addSecondRoundAfterBothLoaded(loaded)));

                // then
                assertThat(failures).isEmpty();
                List<GatheringRoundJpaEntity> stored =
                        gatheringRoundJpaRepository.findAllByGatheringIdOrderByRoundNumber(
                                gatheringId);
                assertThat(stored).hasSize(2);
                assertThat(savedRoundIds).hasSize(2).containsOnly(stored.getLast().getId());
            }

            // 두 요청이 모두 같은 모임을 읽은 뒤에 저장하도록 맞춘다.
            private Long addSecondRoundAfterBothLoaded(CountDownLatch loaded) {
                return transactionTemplate.execute(
                        _ -> {
                            Gathering gathering =
                                    gatheringRepository.findById(gatheringId).orElseThrow();
                            gathering.addRound(GatheringRoundFixture.SECOND.getNumber());
                            loaded.countDown();
                            await(loaded);

                            return gatheringRepository
                                    .save(gathering)
                                    .findRound(GatheringRoundFixture.SECOND.getNumber())
                                    .orElseThrow()
                                    .getId();
                        });
            }
        }

        @Nested
        @DisplayName("같은 모임에 같은 휴대폰의 참여자가 둘인 경우")
        class WhenPhoneNumberDuplicated {

            @Test
            @DisplayName("ParticipantAlreadyJoinedException이 발생한다")
            void shouldThrowParticipantAlreadyJoinedException() {
                // given: 동시 참여로 Service의 중복 검사를 둘 다 통과한 상태
                Gathering gathering =
                        GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                List.of(
                                        ParticipantFixture.KIM.create(),
                                        ParticipantFixture.LEE.createWithPhoneNumber(
                                                ParticipantFixture.KIM.getPhoneNumber())));

                // when & then
                assertThatThrownBy(() -> gatheringRepository.save(gathering))
                        .isInstanceOf(ParticipantAlreadyJoinedException.class);
            }
        }
    }

    @Nested
    @DisplayName("delete 메서드는")
    class Delete {

        @Test
        @DisplayName("모임과 참여자와 차수를 지우고 다른 모임의 차수는 남긴다")
        void shouldDeleteGatheringWithRounds() {
            // given: 파기 1단계로 데이터 키를 지운 상태
            Gathering deleted = saveWithSecondRound();
            Gathering retained = saveWithSecondRound();
            gatheringRepository.discardDataKey(deleted.getId());
            clearPersistenceContext();

            // when
            gatheringRepository.delete(deleted.getId());

            // then
            clearPersistenceContext();
            assertThat(gatheringRepository.findById(deleted.getId())).isEmpty();
            assertThat(
                            gatheringRoundJpaRepository.findAllByGatheringIdOrderByRoundNumber(
                                    deleted.getId()))
                    .isEmpty();
            assertThat(
                            gatheringRoundJpaRepository.findAllByGatheringIdOrderByRoundNumber(
                                    retained.getId()))
                    .hasSize(2);
        }

        private Gathering saveWithSecondRound() {
            Gathering gathering =
                    GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                            List.of(ParticipantFixture.KIM.create()));
            gathering.addRound(GatheringRoundFixture.SECOND.getNumber());

            return gatheringRepository.save(gathering);
        }
    }

    @Nested
    @DisplayName("deleteParticipant 메서드는")
    class DeleteParticipant {

        @Test
        @DisplayName("참여자 한 명만 지우고 같은 모임의 다른 참여자는 남긴다")
        void shouldDeleteOnlyParticipant() {
            // given
            Gathering gathering = saveWithKimAndLee();
            Long kimId = gathering.getParticipants().get(0).getId();
            Long leeId = gathering.getParticipants().get(1).getId();

            // when
            gatheringRepository.deleteParticipant(kimId);

            // then
            clearPersistenceContext();
            Gathering found = gatheringRepository.findById(gathering.getId()).orElseThrow();
            assertThat(found.getParticipants())
                    .extracting(Participant::getId)
                    .containsExactly(leeId);
        }

        @Test
        @DisplayName("정산 건의 결제자면 ParticipantUnremovableException이 발생한다")
        void shouldThrowParticipantUnremovableExceptionWhenParticipantIsPayer() {
            // given
            Gathering gathering = saveWithKimAndLee();
            Long kimId = gathering.getParticipants().get(0).getId();
            Long leeId = gathering.getParticipants().get(1).getId();
            recordDinner(gathering, kimId, List.of(kimId, leeId));

            // when & then
            assertThatThrownBy(() -> gatheringRepository.deleteParticipant(kimId))
                    .isInstanceOf(ParticipantUnremovableException.class);
        }

        @Test
        @DisplayName("정산 건의 분담금 인원이면 ParticipantUnremovableException이 발생한다")
        void shouldThrowParticipantUnremovableExceptionWhenParticipantHasShare() {
            // given
            Gathering gathering = saveWithKimAndLee();
            Long kimId = gathering.getParticipants().get(0).getId();
            Long leeId = gathering.getParticipants().get(1).getId();
            recordDinner(gathering, kimId, List.of(kimId, leeId));

            // when & then
            assertThatThrownBy(() -> gatheringRepository.deleteParticipant(leeId))
                    .isInstanceOf(ParticipantUnremovableException.class);
        }

        private Gathering saveWithKimAndLee() {
            return gatheringRepository.save(
                    GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                            List.of(
                                    ParticipantFixture.KIM.create(),
                                    ParticipantFixture.LEE.create())));
        }

        private void recordDinner(
                Gathering gathering, Long payerParticipantId, List<Long> participantIds) {
            settlementRepository.save(
                    SettlementFixture.DINNER.create(
                            gathering.getId(),
                            gathering.getRounds().getFirst().getId(),
                            payerParticipantId,
                            participantIds));
        }
    }

    @Nested
    @DisplayName("findById 메서드는")
    class FindById {

        @Test
        @DisplayName("존재하는 ID면 참여자의 이름과 휴대폰을 복호화한 모임을 반환한다")
        void shouldReturnDecryptedGatheringWhenIdExists() {
            // given
            Gathering saved =
                    gatheringRepository.save(
                            GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                    List.of(ParticipantFixture.KIM.create())));
            clearPersistenceContext();

            // when
            Optional<Gathering> result = gatheringRepository.findById(saved.getId());

            // then
            assertThat(result).isPresent();
            Participant participant = result.get().getParticipants().getFirst();
            assertThat(participant.getName()).isEqualTo(ParticipantFixture.KIM.getName());
            assertThat(participant.getPhoneNumber().value())
                    .isEqualTo(ParticipantFixture.KIM.getPhoneNumber());
        }

        @Test
        @DisplayName("존재하는 ID면 차수를 번호 순서대로 담은 모임을 반환한다")
        void shouldReturnRoundsInNumberOrderWhenIdExists() {
            // given
            Gathering gathering = GatheringFixture.FRIDAY_DINNER.create();
            gathering.addRound(GatheringRoundFixture.SECOND.getNumber());
            Gathering saved = gatheringRepository.save(gathering);
            clearPersistenceContext();

            // when
            Optional<Gathering> result = gatheringRepository.findById(saved.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getRounds())
                    .extracting(GatheringRound::getNumber)
                    .containsExactly(1, 2);
        }

        @Test
        @DisplayName("존재하지 않는 ID면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenIdNotExists() {
            // when
            Optional<Gathering> result = gatheringRepository.findById(-1L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByParticipantId 메서드는")
    class FindByParticipantId {

        @Test
        @DisplayName("존재하는 참여자 ID면 그 참여자가 속한 모임을 반환한다")
        void shouldReturnGatheringWhenParticipantExists() {
            // given
            Gathering saved =
                    gatheringRepository.save(
                            GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                    List.of(
                                            ParticipantFixture.KIM.create(),
                                            ParticipantFixture.LEE.create())));
            Long participantId = saved.getParticipants().getLast().getId();
            clearPersistenceContext();

            // when
            Optional<Gathering> result = gatheringRepository.findByParticipantId(participantId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(saved.getId());
            assertThat(result.get().getParticipants())
                    .extracting(Participant::getName)
                    .containsExactly(
                            ParticipantFixture.KIM.getName(), ParticipantFixture.LEE.getName());
        }

        @Test
        @DisplayName("존재하지 않는 참여자 ID면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenParticipantNotExists() {
            // when
            Optional<Gathering> result = gatheringRepository.findByParticipantId(-1L);

            // then
            assertThat(result).isEmpty();
        }
    }

    // 1차 캐시를 비워 다음 조회가 DB의 암호문을 다시 읽고 복호화하게 한다.
    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
