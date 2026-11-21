package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.query.GetGatheringParticipantsQuery;
import com.symphonia.gathering.application.dto.result.GatheringParticipantResult;
import com.symphonia.gathering.application.dto.result.GatheringResult;
import com.symphonia.gathering.application.dto.result.HostedGatheringResult;
import com.symphonia.gathering.application.dto.result.OngoingGatheringsResult;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.exception.JoinTokenNotFoundException;
import com.symphonia.gathering.domain.exception.NotGatheringHostException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.vo.JoinClosedReason;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.GatheringRetentionFixture;
import com.symphonia.gathering.fixture.HostedGatheringFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.global.config.TimeConfig;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@DisplayName("GatheringQueryService 단위 테스트")
class GatheringQueryServiceTest extends UnitTest {

    private static final Long GATHERING_ID = 10L;
    private static final Long HOST_MEMBER_ID = GatheringFixture.FRIDAY_DINNER.getHostMemberId();
    private static final Long OTHER_MEMBER_ID = 99L;
    private static final Long FIRST_PARTICIPANT_ID = 20L;
    private static final Long SECOND_PARTICIPANT_ID = 21L;
    private static final String JOIN_TOKEN = "join-token";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 18, 0);

    // 참여 기간은 모임을 만든 지 48시간이 되는 순간 닫힌다.
    // 닫히기 1초 전과 닫히는 순간을 경계로 쓴다.
    private static final LocalDateTime OPEN_PERIOD_OPENED_AT = NOW.minusHours(48).plusSeconds(1);
    private static final LocalDateTime CLOSED_PERIOD_OPENED_AT = NOW.minusHours(48);

    private GatheringQueryService gatheringQueryService;

    @Mock private GatheringRepository gatheringRepository;
    @Mock private GatheringJoinTokenRepository gatheringJoinTokenRepository;
    @Mock private GatheringRetentionRepository gatheringRetentionRepository;

    @BeforeEach
    void setUp() {
        gatheringQueryService =
                new GatheringQueryService(
                        gatheringRepository,
                        gatheringJoinTokenRepository,
                        gatheringRetentionRepository,
                        Clock.fixed(
                                NOW.atZone(TimeConfig.SERVICE_ZONE).toInstant(),
                                TimeConfig.SERVICE_ZONE));
    }

    @Nested
    @DisplayName("getByJoinToken 메서드는")
    class GetByJoinToken {

        @Nested
        @DisplayName("참여 토큰에 해당하는 모임이 없는 경우")
        class WhenJoinTokenNotFound {

            @Test
            @DisplayName("예외를 던진다")
            void shouldThrowJoinTokenNotFoundException() {
                // given
                given(gatheringJoinTokenRepository.findGatheringIdByValue(JOIN_TOKEN))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> gatheringQueryService.getByJoinToken(JOIN_TOKEN))
                        .isInstanceOf(JoinTokenNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("모임의 파기 시각이 지난 경우")
        class WhenGatheringExpired {

            @Test
            @DisplayName("물리 삭제 전이라도 참여 토큰을 찾지 못한 예외를 던지고 모임을 읽지 않는다")
            void shouldThrowJoinTokenNotFoundException() {
                // given
                given(gatheringJoinTokenRepository.findGatheringIdByValue(JOIN_TOKEN))
                        .willReturn(Optional.of(GATHERING_ID));
                givenRetention(GatheringRetentionFixture.EXPIRED);

                // when & then
                assertThatThrownBy(() -> gatheringQueryService.getByJoinToken(JOIN_TOKEN))
                        .isInstanceOf(JoinTokenNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 되기 직전인 경우")
        class WhenWithinRecordingPeriod {

            @Test
            @DisplayName("참여 가능으로 표시하고 현재 동의 안내 버전을 반환한다")
            void shouldReturnJoinableResult() {
                // given
                givenGatheringOpenedAt(OPEN_PERIOD_OPENED_AT);

                // when
                GatheringResult result = gatheringQueryService.getByJoinToken(JOIN_TOKEN);

                // then
                assertThat(result.name()).isEqualTo(GatheringFixture.FRIDAY_DINNER.getName());
                assertThat(result.joinable()).isTrue();
                assertThat(result.joinClosedReason()).isNull();
                assertThat(result.consentVersion()).isEqualTo(GatheringPolicy.CONSENT_VERSION);
                assertThat(result.deletesAt())
                        .isEqualTo(
                                GatheringRetentionFixture.RETAINED
                                        .create(GATHERING_ID, NOW)
                                        .deletesAt());
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 된 경우")
        class WhenRecordingPeriodClosed {

            @Test
            @DisplayName("참여 불가로 표시하고 사유를 기간 만료로 알린다")
            void shouldReturnPeriodExpiredReason() {
                // given
                givenGatheringOpenedAt(CLOSED_PERIOD_OPENED_AT);

                // when
                GatheringResult result = gatheringQueryService.getByJoinToken(JOIN_TOKEN);

                // then
                assertThat(result.joinable()).isFalse();
                assertThat(result.joinClosedReason()).isEqualTo(JoinClosedReason.PERIOD_EXPIRED);
                assertThat(result.consentVersion()).isEqualTo(GatheringPolicy.CONSENT_VERSION);
            }
        }

        @Nested
        @DisplayName("참여자가 19명이라 한 명 더 참여할 수 있는 경우")
        class WhenParticipantsJustBelowLimit {

            @Test
            @DisplayName("참여 가능으로 표시한다")
            void shouldReturnJoinableResult() {
                // given
                givenGatheringOpenedAt(
                        OPEN_PERIOD_OPENED_AT,
                        participantsOf(GatheringPolicy.MAX_PARTICIPANT_COUNT - 1));

                // when
                GatheringResult result = gatheringQueryService.getByJoinToken(JOIN_TOKEN);

                // then
                assertThat(result.joinable()).isTrue();
            }
        }

        @Nested
        @DisplayName("참여자가 20명이라 가득 찬 경우")
        class WhenParticipantsAtLimit {

            @Test
            @DisplayName("참여 불가로 표시하고 사유를 인원 초과로 알린다")
            void shouldReturnParticipantLimitReachedReason() {
                // given
                givenGatheringOpenedAt(
                        OPEN_PERIOD_OPENED_AT,
                        participantsOf(GatheringPolicy.MAX_PARTICIPANT_COUNT));

                // when
                GatheringResult result = gatheringQueryService.getByJoinToken(JOIN_TOKEN);

                // then
                assertThat(result.joinable()).isFalse();
                assertThat(result.joinClosedReason())
                        .isEqualTo(JoinClosedReason.PARTICIPANT_LIMIT_REACHED);
            }
        }

        @Nested
        @DisplayName("참여 기간이 끝났고 참여자도 가득 찬 경우")
        class WhenPeriodClosedAndParticipantsAtLimit {

            @Test
            @DisplayName("참여할 때 먼저 검증하는 기간 만료를 사유로 알린다")
            void shouldReturnPeriodExpiredReason() {
                // given
                givenGatheringOpenedAt(
                        CLOSED_PERIOD_OPENED_AT,
                        participantsOf(GatheringPolicy.MAX_PARTICIPANT_COUNT));

                // when
                GatheringResult result = gatheringQueryService.getByJoinToken(JOIN_TOKEN);

                // then
                assertThat(result.joinable()).isFalse();
                assertThat(result.joinClosedReason()).isEqualTo(JoinClosedReason.PERIOD_EXPIRED);
            }
        }

        private List<Participant> participantsOf(int count) {
            return ParticipantFixture.createWithIds(
                    LongStream.rangeClosed(1, count).boxed().toList());
        }

        private void givenGatheringOpenedAt(LocalDateTime openedAt) {
            givenGatheringOpenedAt(openedAt, List.of());
        }

        private void givenGatheringOpenedAt(
                LocalDateTime openedAt, List<Participant> participants) {
            given(gatheringJoinTokenRepository.findGatheringIdByValue(JOIN_TOKEN))
                    .willReturn(Optional.of(GATHERING_ID));
            givenRetention(GatheringRetentionFixture.RETAINED);
            given(gatheringRepository.findById(GATHERING_ID))
                    .willReturn(
                            Optional.of(
                                    GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                                            GATHERING_ID, openedAt, participants)));
        }
    }

    @Nested
    @DisplayName("getParticipants 메서드는")
    class GetParticipants {

        @Nested
        @DisplayName("모임이 존재하지 않는 경우")
        class WhenGatheringNotFound {

            @Test
            @DisplayName("예외를 던진다")
            void shouldThrowGatheringNotFoundException() {
                // given
                given(gatheringRetentionRepository.findByGatheringId(GATHERING_ID))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(
                                () -> gatheringQueryService.getParticipants(query(HOST_MEMBER_ID)))
                        .isInstanceOf(GatheringNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("모임의 파기 시각이 지난 경우")
        class WhenGatheringExpired {

            @Test
            @DisplayName("물리 삭제 전이라도 모임을 찾지 못한 예외를 던지고 모임을 읽지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                givenRetention(GatheringRetentionFixture.EXPIRED);

                // when & then
                assertThatThrownBy(
                                () -> gatheringQueryService.getParticipants(query(HOST_MEMBER_ID)))
                        .isInstanceOf(GatheringNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임을 만든 회원이 아닌 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("예외를 던진다")
            void shouldThrowNotGatheringHostException() {
                // given
                givenGatheringWithParticipants();

                // when & then
                assertThatThrownBy(
                                () -> gatheringQueryService.getParticipants(query(OTHER_MEMBER_ID)))
                        .isInstanceOf(NotGatheringHostException.class);
            }
        }

        @Nested
        @DisplayName("모임을 만든 회원인 경우")
        class WhenMemberIsHost {

            @Test
            @DisplayName("참여한 순서대로 id, 이름, 마스킹한 휴대폰을 반환한다")
            void shouldReturnMaskedParticipants() {
                // given
                givenGatheringWithParticipants();

                // when
                List<GatheringParticipantResult> result =
                        gatheringQueryService.getParticipants(query(HOST_MEMBER_ID));

                // then
                assertThat(result)
                        .containsExactly(
                                new GatheringParticipantResult(
                                        FIRST_PARTICIPANT_ID,
                                        ParticipantFixture.KIM.getName(),
                                        "010-****-5678"),
                                new GatheringParticipantResult(
                                        SECOND_PARTICIPANT_ID,
                                        ParticipantFixture.LEE.getName(),
                                        "010-****-5432"));
            }
        }

        private GetGatheringParticipantsQuery query(Long memberId) {
            return GetGatheringParticipantsQuery.of(memberId, GATHERING_ID);
        }

        private void givenGatheringWithParticipants() {
            givenRetention(GatheringRetentionFixture.RETAINED);
            given(gatheringRepository.findById(GATHERING_ID))
                    .willReturn(
                            Optional.of(
                                    GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                                            GATHERING_ID,
                                            OPEN_PERIOD_OPENED_AT,
                                            List.of(
                                                    ParticipantFixture.KIM.createWithId(
                                                            FIRST_PARTICIPANT_ID),
                                                    ParticipantFixture.LEE.createWithId(
                                                            SECOND_PARTICIPANT_ID)))));
        }
    }

    @Nested
    @DisplayName("getOngoing 메서드는")
    class GetOngoing {

        private static final Long MEMBER_ID = 1L;

        @Nested
        @DisplayName("모임장으로 연 모임이 없는 경우")
        class WhenNoGatherings {

            @Test
            @DisplayName("모임 수 0과 파기 예정 시각 없음을 반환한다")
            void shouldReturnZeroCountAndNoDeletesAt() {
                // given
                given(gatheringRetentionRepository.findAllByHostMemberId(MEMBER_ID))
                        .willReturn(List.of());

                // when
                OngoingGatheringsResult result = gatheringQueryService.getOngoing(MEMBER_ID);

                // then
                assertThat(result.count()).isZero();
                assertThat(result.lastDeletesAt()).isNull();
            }
        }

        @Nested
        @DisplayName("파기 시각이 지난 모임이 섞여 있는 경우")
        class WhenExpiredGatheringsMixed {

            @Test
            @DisplayName("파기 시각이 지난 모임과 데이터 키를 지운 모임을 빼고 세고 가장 늦은 파기 예정 시각을 반환한다")
            void shouldCountOnlyOngoingGatherings() {
                // given
                given(gatheringRetentionRepository.findAllByHostMemberId(MEMBER_ID))
                        .willReturn(
                                List.of(
                                        GatheringRetentionFixture.ABOUT_TO_EXPIRE.create(
                                                GATHERING_ID, NOW),
                                        GatheringRetentionFixture.RETAINED.create(
                                                GATHERING_ID + 1, NOW),
                                        GatheringRetentionFixture.EXPIRED.create(
                                                GATHERING_ID + 2, NOW),
                                        GatheringRetentionFixture.DATA_KEY_DISCARDED.create(
                                                GATHERING_ID + 3, NOW)));

                // when
                OngoingGatheringsResult result = gatheringQueryService.getOngoing(MEMBER_ID);

                // then
                assertThat(result.count()).isEqualTo(2);
                assertThat(result.lastDeletesAt()).isEqualTo(NOW.plusDays(7));
            }
        }
    }

    @Nested
    @DisplayName("getHosted 메서드는")
    class GetHosted {

        private static final Long MEMBER_ID = 1L;

        @Nested
        @DisplayName("모임장으로 연 모임이 없는 경우")
        class WhenNoGatherings {

            @Test
            @DisplayName("빈 목록을 반환한다")
            void shouldReturnEmptyList() {
                // given
                given(gatheringRetentionRepository.findAllByHostMemberId(MEMBER_ID))
                        .willReturn(List.of());

                // when
                List<HostedGatheringResult> results = gatheringQueryService.getHosted(MEMBER_ID);

                // then
                assertThat(results).isEmpty();
            }
        }

        @Nested
        @DisplayName("파기 시각이 지난 모임이 섞여 있는 경우")
        class WhenExpiredGatheringsMixed {

            @Test
            @DisplayName("파기 시각이 지난 모임과 데이터 키를 지운 모임을 빼고 읽은 순서대로 반환한다")
            void shouldReturnOnlyOngoingGatheringsInReadOrder() {
                // given
                given(gatheringRetentionRepository.findAllByHostMemberId(MEMBER_ID))
                        .willReturn(
                                List.of(
                                        GatheringRetentionFixture.ABOUT_TO_EXPIRE.create(
                                                GATHERING_ID, NOW),
                                        GatheringRetentionFixture.EXPIRED.create(
                                                GATHERING_ID + 1, NOW),
                                        GatheringRetentionFixture.DATA_KEY_DISCARDED.create(
                                                GATHERING_ID + 2, NOW),
                                        GatheringRetentionFixture.RETAINED.create(
                                                GATHERING_ID + 3, NOW)));
                given(
                                gatheringRepository.findAllHostedByIds(
                                        argThat(
                                                ids ->
                                                        Set.copyOf(ids)
                                                                .equals(
                                                                        Set.of(
                                                                                GATHERING_ID,
                                                                                GATHERING_ID
                                                                                        + 3)))))
                        .willReturn(
                                List.of(
                                        HostedGatheringFixture.FRIDAY_DINNER.create(
                                                GATHERING_ID + 3),
                                        HostedGatheringFixture.EMPTY_GATHERING.create(
                                                GATHERING_ID)));

                // when
                List<HostedGatheringResult> results = gatheringQueryService.getHosted(MEMBER_ID);

                // then
                assertThat(results)
                        .extracting(HostedGatheringResult::gatheringId)
                        .containsExactly(GATHERING_ID + 3, GATHERING_ID);
                assertThat(results)
                        .extracting(HostedGatheringResult::deletesAt)
                        .containsExactly(NOW.plusDays(7), NOW.plusSeconds(1));
            }
        }

        @Nested
        @DisplayName("기록 기간을 연장한 모임인 경우")
        class WhenRecordingExtended {

            @Test
            @DisplayName("파기 시각을 읽은 조회의 연장 여부로 기록 기간이 끝나는 시각을 담아 반환한다")
            void shouldReturnRecordingClosesAtFromRetention() {
                // given
                LocalDateTime openedAt = NOW;
                given(gatheringRetentionRepository.findAllByHostMemberId(MEMBER_ID))
                        .willReturn(
                                List.of(
                                        GatheringRetentionFixture.RETAINED.create(
                                                GATHERING_ID, NOW),
                                        GatheringRetentionFixture.EXTENDED.create(
                                                GATHERING_ID + 1, NOW)));
                given(gatheringRepository.findAllHostedByIds(anyList()))
                        .willReturn(
                                List.of(
                                        HostedGatheringFixture.FRIDAY_DINNER.create(
                                                GATHERING_ID + 1),
                                        HostedGatheringFixture.FRIDAY_DINNER.create(GATHERING_ID)));

                // when
                List<HostedGatheringResult> results = gatheringQueryService.getHosted(MEMBER_ID);

                // then
                assertThat(results)
                        .extracting(
                                HostedGatheringResult::recordingExtended,
                                HostedGatheringResult::recordingClosesAt,
                                HostedGatheringResult::participantCount)
                        .containsExactly(
                                tuple(true, openedAt.plusHours(96), 3L),
                                tuple(false, openedAt.plusHours(48), 3L));
            }
        }
    }

    @Nested
    @DisplayName("getExpiredGatheringIds 메서드는")
    class GetExpiredGatheringIds {

        @Test
        @DisplayName("기록 기간 48시간과 정산이 끝난 뒤 7일이 모두 지났을 수 있는 모임 중 파기 시각이 된 모임만 반환한다")
        void shouldReturnOnlyExpiredGatherings() {
            // given
            Long retainedGatheringId = GATHERING_ID + 1;
            Long discardedGatheringId = GATHERING_ID + 2;
            // 연장은 파기를 늦추기만 하므로 연장하지 않은 모임을 놓치지 않게 가장 짧은 기록 기간(48시간)을 기준으로 후보를 읽는다.
            given(gatheringRetentionRepository.findAllOpenedUntil(NOW.minusDays(7).minusHours(48)))
                    .willReturn(
                            List.of(
                                    GatheringRetentionFixture.EXPIRED.create(GATHERING_ID, NOW),
                                    GatheringRetentionFixture.RETAINED.create(
                                            retainedGatheringId, NOW),
                                    GatheringRetentionFixture.DATA_KEY_DISCARDED.create(
                                            discardedGatheringId, NOW)));

            // when
            List<Long> result = gatheringQueryService.getExpiredGatheringIds();

            // then
            assertThat(result).containsExactly(GATHERING_ID, discardedGatheringId);
        }
    }

    private void givenRetention(GatheringRetentionFixture retention) {
        given(gatheringRetentionRepository.findByGatheringId(GATHERING_ID))
                .willReturn(Optional.of(retention.create(GATHERING_ID, NOW)));
    }
}
