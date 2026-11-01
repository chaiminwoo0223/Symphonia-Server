package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.result.GatheringResult;
import com.symphonia.gathering.domain.exception.JoinTokenNotFoundException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.global.config.TimeConfig;
import java.time.Clock;
import java.time.LocalDateTime;
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
    private static final LocalDateTime OPEN_CREATED_AT = NOW.minusHours(48).plusSeconds(1);
    private static final LocalDateTime CLOSED_CREATED_AT = NOW.minusHours(48);

    private GatheringQueryService gatheringQueryService;

    @Mock private GatheringRepository gatheringRepository;
    @Mock private GatheringJoinTokenRepository gatheringJoinTokenRepository;

    @BeforeEach
    void setUp() {
        gatheringQueryService =
                new GatheringQueryService(
                        gatheringRepository,
                        gatheringJoinTokenRepository,
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
        @DisplayName("모임을 만든 지 48시간이 되기 직전인 경우")
        class WhenWithinRecordingPeriod {

            @Test
            @DisplayName("참여 가능으로 표시하고 현재 동의 안내 버전을 반환한다")
            void shouldReturnJoinableResult() {
                // given
                givenGatheringCreatedAt(OPEN_CREATED_AT);

                // when
                GatheringResult result = gatheringQueryService.getByJoinToken(JOIN_TOKEN);

                // then
                assertThat(result.name()).isEqualTo(GatheringFixture.FRIDAY_DINNER.getName());
                assertThat(result.joinable()).isTrue();
                assertThat(result.consentVersion()).isEqualTo(GatheringPolicy.CONSENT_VERSION);
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 된 경우")
        class WhenRecordingPeriodPassed {

            @Test
            @DisplayName("참여 불가로 표시한다")
            void shouldReturnNotJoinableResult() {
                // given
                givenGatheringCreatedAt(CLOSED_CREATED_AT);

                // when
                GatheringResult result = gatheringQueryService.getByJoinToken(JOIN_TOKEN);

                // then
                assertThat(result.joinable()).isFalse();
                assertThat(result.consentVersion()).isEqualTo(GatheringPolicy.CONSENT_VERSION);
            }
        }

        private void givenGatheringCreatedAt(LocalDateTime createdAt) {
            given(gatheringJoinTokenRepository.findGatheringIdByValue(JOIN_TOKEN))
                    .willReturn(Optional.of(GATHERING_ID));
            given(gatheringRepository.findById(GATHERING_ID))
                    .willReturn(
                            Optional.of(
                                    GatheringFixture.FRIDAY_DINNER.createWithIdAndCreatedAt(
                                            GATHERING_ID, createdAt)));
        }
    }
}
