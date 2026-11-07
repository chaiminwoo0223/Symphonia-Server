package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.result.GatheringResult;
import com.symphonia.gathering.domain.exception.JoinTokenNotFoundException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.GatheringRetentionFixture;
import com.symphonia.global.config.TimeConfig;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@DisplayName("GatheringQueryService 단위 테스트")
class GatheringQueryServiceTest extends UnitTest {

    private static final Long GATHERING_ID = 10L;
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
            @DisplayName("참여 불가로 표시한다")
            void shouldReturnNotJoinableResult() {
                // given
                givenGatheringOpenedAt(CLOSED_PERIOD_OPENED_AT);

                // when
                GatheringResult result = gatheringQueryService.getByJoinToken(JOIN_TOKEN);

                // then
                assertThat(result.joinable()).isFalse();
                assertThat(result.consentVersion()).isEqualTo(GatheringPolicy.CONSENT_VERSION);
            }
        }

        private void givenGatheringOpenedAt(LocalDateTime openedAt) {
            given(gatheringJoinTokenRepository.findGatheringIdByValue(JOIN_TOKEN))
                    .willReturn(Optional.of(GATHERING_ID));
            givenRetention(GatheringRetentionFixture.RETAINED);
            given(gatheringRepository.findById(GATHERING_ID))
                    .willReturn(
                            Optional.of(
                                    GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                                            GATHERING_ID, openedAt)));
        }

        private void givenRetention(GatheringRetentionFixture retention) {
            given(gatheringRetentionRepository.findByGatheringId(GATHERING_ID))
                    .willReturn(Optional.of(retention.create(GATHERING_ID, NOW)));
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
}
