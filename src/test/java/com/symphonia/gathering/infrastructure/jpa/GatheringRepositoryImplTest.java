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
import com.symphonia.gathering.domain.exception.ParticipantPhoneNumberAlreadyUsedException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.vo.HostedGathering;
import com.symphonia.gathering.domain.vo.PhoneNumber;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.GatheringRoundFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.infrastructure.crypto.GatheringMasterKey;
import com.symphonia.gathering.infrastructure.crypto.config.GatheringCryptoConfig;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
    GatheringDataKeyLoader.class,
    GatheringMasterKey.class,
    GatheringCryptoConfig.class
})
class GatheringRepositoryImplTest extends RepositoryTest {

    private static final LocalDateTime WITHDRAWN_AT = LocalDateTime.of(2026, 11, 22, 10, 0);

    @Autowired private GatheringRepository gatheringRepository;
    @Autowired private ParticipantJpaRepository participantJpaRepository;
    @Autowired private GatheringDataKeyJpaRepository gatheringDataKeyJpaRepository;
    @Autowired private GatheringRoundJpaRepository gatheringRoundJpaRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Nested
    @DisplayName("findAllHostedByIds 메서드는")
    class FindAllHostedByIds {

        @Test
        @DisplayName("요청한 모임만 id가 큰 순서로 이름과 참여자 수와 함께 반환한다")
        void shouldReturnRequestedGatheringsInDescendingIdOrder() {
            // given
            Gathering first = gatheringRepository.save(GatheringFixture.FRIDAY_DINNER.create());
            Gathering second =
                    gatheringRepository.save(
                            GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                    List.of(
                                            ParticipantFixture.KIM.create(),
                                            ParticipantFixture.LEE.create())));
            Gathering notRequested =
                    gatheringRepository.save(GatheringFixture.FRIDAY_DINNER.create());
            clearPersistenceContext();

            // when
            List<HostedGathering> results =
                    gatheringRepository.findAllHostedByIds(List.of(first.getId(), second.getId()));

            // then
            assertThat(results)
                    .extracting(
                            HostedGathering::gatheringId,
                            HostedGathering::name,
                            HostedGathering::participantCount)
                    .containsExactly(
                            tuple(second.getId(), second.getName(), 2L),
                            tuple(first.getId(), first.getName(), 0L));
            assertThat(results)
                    .extracting(HostedGathering::gatheringId)
                    .doesNotContain(notRequested.getId());
        }

        @Test
        @DisplayName("익명화한 참여자는 참여자 수에 세지 않는다")
        void shouldNotCountAnonymizedParticipant() {
            // given
            Gathering gathering =
                    gatheringRepository.save(
                            GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                    List.of(
                                            ParticipantFixture.KIM.create(),
                                            ParticipantFixture.LEE.create())));
            gatheringRepository.anonymizeParticipant(
                    gathering.getParticipants().getFirst().getId(), WITHDRAWN_AT);
            clearPersistenceContext();

            // when
            List<HostedGathering> results =
                    gatheringRepository.findAllHostedByIds(List.of(gathering.getId()));

            // then
            assertThat(results).extracting(HostedGathering::participantCount).containsExactly(1L);
        }

        @Test
        @DisplayName("모임 id 목록이 비어 있으면 빈 목록을 반환한다")
        void shouldReturnEmptyListWhenNoIds() {
            // given
            gatheringRepository.save(GatheringFixture.FRIDAY_DINNER.create());
            clearPersistenceContext();

            // when
            List<HostedGathering> results = gatheringRepository.findAllHostedByIds(List.of());

            // then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("saveIfAbsent 메서드는")
    class SaveIfAbsent {

        private static final UUID CLIENT_REQUEST_ID =
                UUID.fromString("0b6c1a52-6f4e-4d0a-9d52-7a1f3c1e8b11");

        @Test
        @DisplayName("요청 id의 모임이 없으면 모임과 데이터 키와 1차를 저장하고 반환한다")
        void shouldSaveGatheringWithDataKeyAndFirstRound() {
            // given
            Gathering gathering = openedGathering(1L);

            // when
            Optional<Gathering> result = gatheringRepository.saveIfAbsent(gathering);

            // then
            assertThat(result).isPresent();
            Gathering saved = result.orElseThrow();
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getClientRequestId()).isEqualTo(CLIENT_REQUEST_ID);
            assertThat(saved.getName()).isEqualTo(gathering.getName());
            assertThat(saved.getOpenedAt()).isEqualTo(gathering.getOpenedAt());
            assertThat(saved.getRounds()).extracting(GatheringRound::getNumber).containsExactly(1);
            assertThat(gatheringDataKeyJpaRepository.existsById(saved.getId())).isTrue();
        }

        @Test
        @DisplayName("같은 모임장과 요청 id의 모임이 이미 있으면 저장하지 않고 빈 값을 반환한다")
        void shouldReturnEmptyWhenSameHostAndClientRequestId() {
            // given
            Gathering first = gatheringRepository.saveIfAbsent(openedGathering(1L)).orElseThrow();
            clearPersistenceContext();

            // when
            Optional<Gathering> result = gatheringRepository.saveIfAbsent(openedGathering(1L));

            // then
            assertThat(result).isEmpty();
            assertThat(
                            gatheringRepository
                                    .findByHostMemberIdAndClientRequestId(1L, CLIENT_REQUEST_ID)
                                    .orElseThrow()
                                    .getId())
                    .isEqualTo(first.getId());
        }

        @Test
        @DisplayName("모임장이 다르면 같은 요청 id여도 저장한다")
        void shouldSaveWhenOtherHost() {
            // given
            gatheringRepository.saveIfAbsent(openedGathering(1L));
            clearPersistenceContext();

            // when
            Optional<Gathering> result = gatheringRepository.saveIfAbsent(openedGathering(2L));

            // then
            assertThat(result).isPresent();
        }

        private Gathering openedGathering(Long hostMemberId) {
            return Gathering.open(
                    hostMemberId,
                    CLIENT_REQUEST_ID,
                    GatheringFixture.FRIDAY_DINNER.getName(),
                    LocalDateTime.of(2026, 11, 21, 18, 0));
        }
    }

    @Nested
    @DisplayName("findHostMemberIdByIdForUpdate 메서드는")
    class FindHostMemberIdByIdForUpdate {

        @Test
        @DisplayName("모임장의 회원 id를 반환한다")
        void shouldReturnHostMemberId() {
            // given
            Gathering saved = gatheringRepository.save(GatheringFixture.FRIDAY_DINNER.create());

            // when
            Optional<Long> result =
                    gatheringRepository.findHostMemberIdByIdForUpdate(saved.getId());

            // then
            assertThat(result).contains(saved.getHostMemberId());
        }

        @Test
        @DisplayName("데이터 키를 지운 모임도 복호화하지 않고 모임장의 회원 id를 반환한다")
        void shouldReturnHostMemberIdWhenDataKeyDiscarded() {
            // given
            Gathering saved =
                    gatheringRepository.save(
                            GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                    List.of(ParticipantFixture.KIM.create())));
            gatheringRepository.discardDataKey(saved.getId());
            clearPersistenceContext();

            // when
            Optional<Long> result =
                    gatheringRepository.findHostMemberIdByIdForUpdate(saved.getId());

            // then
            assertThat(result).contains(saved.getHostMemberId());
        }

        @Test
        @DisplayName("없는 모임이면 빈 값을 반환한다")
        void shouldReturnEmptyWhenGatheringNotFound() {
            // when
            Optional<Long> result = gatheringRepository.findHostMemberIdByIdForUpdate(0L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateName 메서드는")
    class UpdateName {

        @Test
        @DisplayName("이름을 바꾸고 다시 읽으면 바뀐 이름을 반환한다")
        void shouldPersistName() {
            // given
            Gathering saved = gatheringRepository.save(GatheringFixture.FRIDAY_DINNER.create());

            // when
            gatheringRepository.updateName(saved.getId(), "토요일 번개");

            // then
            clearPersistenceContext();
            assertThat(gatheringRepository.findById(saved.getId()).orElseThrow().getName())
                    .isEqualTo("토요일 번개");
        }

        @Test
        @DisplayName("이름을 바꾸기 전에 읽은 모임을 바꾼 뒤에 저장해도 이름을 되돌리지 않는다")
        void shouldKeepNameWhenStaleGatheringSaved() {
            // given
            Gathering staleGathering =
                    gatheringRepository.save(GatheringFixture.FRIDAY_DINNER.create());
            clearPersistenceContext();
            gatheringRepository.updateName(staleGathering.getId(), "토요일 번개");
            clearPersistenceContext();

            // when
            gatheringRepository.save(staleGathering);

            // then
            clearPersistenceContext();
            assertThat(gatheringRepository.findById(staleGathering.getId()).orElseThrow().getName())
                    .isEqualTo("토요일 번개");
        }
    }

    @Nested
    @DisplayName("updateParticipant 메서드는")
    class UpdateParticipant {

        @Test
        @DisplayName("참여자의 이름과 휴대폰을 바꾸고 다시 읽으면 바뀐 값을 반환하며 다른 참여자는 그대로 둔다")
        void shouldPersistNameAndPhoneNumber() {
            // given
            Gathering saved = saveWithKimAndLee();
            Participant kim = saved.getParticipants().get(0);
            saved.correctParticipant(kim.getId(), "김영수", new PhoneNumber("01077778888"));

            // when
            gatheringRepository.updateParticipant(saved.getId(), saved.getParticipant(kim.getId()));

            // then
            clearPersistenceContext();
            List<Participant> participants =
                    gatheringRepository.findById(saved.getId()).orElseThrow().getParticipants();
            assertThat(participants)
                    .extracting(
                            Participant::getId,
                            Participant::getName,
                            participant -> participant.getPhoneNumber().value())
                    .containsExactly(
                            tuple(kim.getId(), "김영수", "01077778888"),
                            tuple(
                                    saved.getParticipants().get(1).getId(),
                                    ParticipantFixture.LEE.getName(),
                                    ParticipantFixture.LEE.getPhoneNumber()));
        }

        @Test
        @DisplayName("바꾼 이름과 휴대폰도 평문 없이 암호화해 저장하고 휴대폰 중복 검사용 HMAC을 함께 바꾼다")
        void shouldEncryptNameAndPhoneNumberAndReplaceHmac() {
            // given
            Gathering saved = saveWithKimAndLee();
            Participant kim = saved.getParticipants().get(0);
            String oldHmac = findPhoneNumberHmac(kim.getId());
            saved.correctParticipant(kim.getId(), "김영수", new PhoneNumber("01077778888"));

            // when
            gatheringRepository.updateParticipant(saved.getId(), saved.getParticipant(kim.getId()));

            // then
            clearPersistenceContext();
            ParticipantJpaEntity stored =
                    participantJpaRepository.findById(kim.getId()).orElseThrow();
            assertThat(new String(stored.getEncryptedName(), StandardCharsets.UTF_8))
                    .doesNotContain("김영수");
            assertThat(new String(stored.getEncryptedPhoneNumber(), StandardCharsets.UTF_8))
                    .doesNotContain("01077778888");
            assertThat(stored.getPhoneNumberHmac())
                    .isNotEqualTo(oldHmac)
                    .doesNotContain("01077778888");
        }

        @Test
        @DisplayName("같은 모임의 다른 참여자가 쓰는 휴대폰으로 바꾸면 ParticipantPhoneNumberAlreadyUsedException이 발생한다")
        void shouldThrowParticipantPhoneNumberAlreadyUsedExceptionWhenPhoneNumberUsedByOther() {
            // given
            Gathering saved = saveWithKimAndLee();
            Participant kim = saved.getParticipants().get(0);
            Participant kimWithLeePhoneNumber =
                    Participant.builder()
                            .id(kim.getId())
                            .name(kim.getName())
                            .phoneNumber(new PhoneNumber(ParticipantFixture.LEE.getPhoneNumber()))
                            .consentVersion(kim.getConsentVersion())
                            .consentedAt(kim.getConsentedAt())
                            .build();

            // when & then
            assertThatThrownBy(
                            () ->
                                    gatheringRepository.updateParticipant(
                                            saved.getId(), kimWithLeePhoneNumber))
                    .isInstanceOf(ParticipantPhoneNumberAlreadyUsedException.class);
        }

        private Gathering saveWithKimAndLee() {
            return gatheringRepository.save(
                    GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                            List.of(
                                    ParticipantFixture.KIM.create(),
                                    ParticipantFixture.LEE.create())));
        }

        private String findPhoneNumberHmac(Long participantId) {
            return participantJpaRepository
                    .findById(participantId)
                    .orElseThrow()
                    .getPhoneNumberHmac();
        }
    }

    @Nested
    @DisplayName("markRecordingExtended 메서드는")
    class MarkRecordingExtended {

        @Test
        @DisplayName("연장했다고 표시하고 다시 읽으면 연장한 모임으로 반환한다")
        void shouldPersistRecordingExtended() {
            // given
            Gathering saved = gatheringRepository.save(GatheringFixture.FRIDAY_DINNER.create());

            // when
            gatheringRepository.markRecordingExtended(saved.getId());

            // then
            clearPersistenceContext();
            assertThat(
                            gatheringRepository
                                    .findById(saved.getId())
                                    .orElseThrow()
                                    .isRecordingExtended())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Nested
        @DisplayName("연장하기 전에 읽은 모임을 연장한 뒤에 저장하는 경우")
        class WhenRecordingExtendedAfterRead {

            @Test
            @DisplayName("연장 여부를 되돌리지 않는다")
            void shouldKeepRecordingExtended() {
                // given
                Gathering staleGathering =
                        gatheringRepository.save(GatheringFixture.FRIDAY_DINNER.create());
                clearPersistenceContext();
                gatheringRepository.markRecordingExtended(staleGathering.getId());
                clearPersistenceContext();

                // when
                gatheringRepository.save(staleGathering);

                // then
                clearPersistenceContext();
                assertThat(
                                gatheringRepository
                                        .findById(staleGathering.getId())
                                        .orElseThrow()
                                        .isRecordingExtended())
                        .isTrue();
            }
        }

        @Nested
        @DisplayName("처음 저장하는 모임인 경우")
        class WhenGatheringIsNew {

            @Test
            @DisplayName("연장하지 않은 모임으로 저장한다")
            void shouldPersistAsNotExtended() {
                // given
                Gathering gathering = GatheringFixture.FRIDAY_DINNER.create();

                // when
                Gathering saved = gatheringRepository.save(gathering);

                // then
                clearPersistenceContext();
                assertThat(
                                gatheringRepository
                                        .findById(saved.getId())
                                        .orElseThrow()
                                        .isRecordingExtended())
                        .isFalse();
            }

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
            @DisplayName("ParticipantPhoneNumberAlreadyUsedException이 발생한다")
            void shouldThrowParticipantPhoneNumberAlreadyUsedException() {
                // given: 동시 참여로 Service의 중복 검사를 둘 다 통과한 상태
                Gathering gathering =
                        GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                List.of(
                                        ParticipantFixture.KIM.create(),
                                        ParticipantFixture.LEE.createWithPhoneNumber(
                                                ParticipantFixture.KIM.getPhoneNumber())));

                // when & then
                assertThatThrownBy(() -> gatheringRepository.save(gathering))
                        .isInstanceOf(ParticipantPhoneNumberAlreadyUsedException.class);
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

        private Gathering saveWithKimAndLee() {
            return gatheringRepository.save(
                    GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                            List.of(
                                    ParticipantFixture.KIM.create(),
                                    ParticipantFixture.LEE.create())));
        }
    }

    @Nested
    @DisplayName("anonymizeParticipant 메서드는")
    class AnonymizeParticipant {

        @Test
        @DisplayName("익명화한 참여자는 모임을 읽을 때 불러오지 않고 같은 모임의 다른 참여자는 남긴다")
        void shouldExcludeAnonymizedParticipantFromGathering() {
            // given
            Gathering gathering = saveWithKimAndLee();
            Long kimId = gathering.getParticipants().get(0).getId();
            Long leeId = gathering.getParticipants().get(1).getId();

            // when
            gatheringRepository.anonymizeParticipant(kimId, WITHDRAWN_AT);

            // then
            clearPersistenceContext();
            Gathering found = gatheringRepository.findById(gathering.getId()).orElseThrow();
            assertThat(found.getParticipants())
                    .extracting(Participant::getId)
                    .containsExactly(leeId);
        }

        @Test
        @DisplayName("행은 지우지 않고 이름, 휴대폰, 휴대폰 HMAC, 구글 계정 값을 비우고 철회 시각을 남긴다")
        void shouldKeepRowWithPersonalDataCleared() {
            // given
            Gathering gathering = saveWithKimAndLee();
            Long kimId = gathering.getParticipants().get(0).getId();
            Long leeId = gathering.getParticipants().get(1).getId();
            jdbcTemplate.update(
                    "UPDATE participant SET google_sub_hmac = ? WHERE id = ?",
                    "a".repeat(64),
                    kimId);

            // when
            gatheringRepository.anonymizeParticipant(kimId, WITHDRAWN_AT);

            // then
            clearPersistenceContext();
            List<ParticipantJpaEntity> rows =
                    participantJpaRepository.findAllByGatheringIdOrderById(gathering.getId());
            assertThat(rows).extracting(ParticipantJpaEntity::getId).containsExactly(kimId, leeId);
            ParticipantJpaEntity anonymized = rows.getFirst();
            assertThat(anonymized.getEncryptedName()).isNull();
            assertThat(anonymized.getEncryptedPhoneNumber()).isNull();
            assertThat(anonymized.getPhoneNumberHmac()).isNull();
            assertThat(anonymized.getGoogleSubHmac()).isNull();
            assertThat(anonymized.getWithdrawnAt()).isEqualTo(WITHDRAWN_AT);
        }

        @Test
        @DisplayName("같은 모임의 다른 참여자의 값은 바꾸지 않는다")
        void shouldNotChangeOtherParticipant() {
            // given
            Gathering gathering = saveWithKimAndLee();
            Long kimId = gathering.getParticipants().get(0).getId();
            Long leeId = gathering.getParticipants().get(1).getId();

            // when
            gatheringRepository.anonymizeParticipant(kimId, WITHDRAWN_AT);

            // then
            clearPersistenceContext();
            ParticipantJpaEntity lee = participantJpaRepository.findById(leeId).orElseThrow();
            assertThat(lee.getEncryptedName()).isNotNull();
            assertThat(lee.getEncryptedPhoneNumber()).isNotNull();
            assertThat(lee.getPhoneNumberHmac()).isNotNull();
            assertThat(lee.getWithdrawnAt()).isNull();
        }

        @Test
        @DisplayName("익명화한 뒤 같은 휴대폰으로 다시 참여해 저장할 수 있다")
        void shouldAllowRejoinWithSamePhoneNumber() {
            // given
            Gathering gathering = saveWithKimAndLee();
            Long kimId = gathering.getParticipants().get(0).getId();
            gatheringRepository.anonymizeParticipant(kimId, WITHDRAWN_AT);
            clearPersistenceContext();
            Gathering loaded = gatheringRepository.findById(gathering.getId()).orElseThrow();
            loaded.join(
                    ParticipantFixture.KIM.getName(),
                    new PhoneNumber(ParticipantFixture.KIM.getPhoneNumber()),
                    GatheringPolicy.CONSENT_VERSION,
                    loaded.getOpenedAt().plusHours(1));

            // when
            Gathering saved = gatheringRepository.save(loaded);

            // then
            clearPersistenceContext();
            Gathering found = gatheringRepository.findById(saved.getId()).orElseThrow();
            assertThat(found.getParticipants())
                    .extracting(Participant::getName)
                    .containsExactly(
                            ParticipantFixture.LEE.getName(), ParticipantFixture.KIM.getName());
            assertThat(found.getParticipants())
                    .extracting(Participant::getId)
                    .doesNotContain(kimId);
        }

        private Gathering saveWithKimAndLee() {
            return gatheringRepository.save(
                    GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                            List.of(
                                    ParticipantFixture.KIM.create(),
                                    ParticipantFixture.LEE.create())));
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
    @DisplayName("findByIdForUpdate 메서드는")
    class FindByIdForUpdate {

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
            Optional<Gathering> result = gatheringRepository.findByIdForUpdate(saved.getId());

            // then
            assertThat(result).isPresent();
            Participant participant = result.get().getParticipants().getFirst();
            assertThat(participant.getName()).isEqualTo(ParticipantFixture.KIM.getName());
            assertThat(participant.getPhoneNumber().value())
                    .isEqualTo(ParticipantFixture.KIM.getPhoneNumber());
        }

        @Test
        @DisplayName("존재하지 않는 ID면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenIdNotExists() {
            // when
            Optional<Gathering> result = gatheringRepository.findByIdForUpdate(-1L);

            // then
            assertThat(result).isEmpty();
        }

        // 두 트랜잭션이 실제로 겹쳐야 하므로 테스트 트랜잭션 없이 실행하고, 커밋된 행은 테스트가 끝나면 지운다.
        @Nested
        @DisplayName("두 요청이 같은 모임을 동시에 잠그고 읽는 경우")
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        class WhenLockedConcurrently {

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
            @DisplayName("늦은 요청이 먼저 잡은 요청이 끝날 때까지 기다려 두 요청이 겹치지 않는다")
            void shouldNotOverlapLockedSections() {
                // given
                gatheringId =
                        transactionTemplate.execute(
                                _ ->
                                        gatheringRepository
                                                .save(GatheringFixture.FRIDAY_DINNER.create())
                                                .getId());
                CountDownLatch ready = new CountDownLatch(2);
                List<String> events = new CopyOnWriteArrayList<>();

                // when
                List<Throwable> failures =
                        runConcurrently(
                                () -> holdLock(ready, events), () -> holdLock(ready, events));

                // then
                assertThat(failures).isEmpty();
                assertThat(events).containsExactly("enter", "exit", "enter", "exit");
            }

            // 잠금 구간 안에서 잠시 머물러, 잠금이 없다면 두 요청이 서로의 구간 안에 들어오게 한다.
            private void holdLock(CountDownLatch ready, List<String> events) {
                ready.countDown();
                await(ready);
                transactionTemplate.executeWithoutResult(
                        _ -> {
                            gatheringRepository.findByIdForUpdate(gatheringId).orElseThrow();
                            events.add("enter");
                            sleep(300);
                            events.add("exit");
                        });
            }

            private void sleep(long millis) {
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }
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
