package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.result.GatheringResult;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.fixture.GatheringFixture;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("GatheringQueryService 단위 테스트")
class GatheringQueryServiceTest extends UnitTest {

    private static final Long GATHERING_ID = 10L;
    private static final String JOIN_TOKEN = "join-token";

    @InjectMocks private GatheringQueryService gatheringQueryService;

    @Mock private GatheringRepository gatheringRepository;
    @Mock private GatheringJoinTokenRepository gatheringJoinTokenRepository;

    @Nested
    @DisplayName("getByJoinToken 메서드는")
    class GetByJoinToken {

        @Nested
        @DisplayName("참여 토큰에 해당하는 모임이 없는 경우")
        class WhenJoinTokenNotFound {

            @Test
            @DisplayName("예외를 던진다")
            void shouldThrowGatheringNotFoundException() {
                // given
                given(gatheringJoinTokenRepository.findGatheringIdByValue(JOIN_TOKEN))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> gatheringQueryService.getByJoinToken(JOIN_TOKEN))
                        .isInstanceOf(GatheringNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 지나지 않은 경우")
        class WhenWithinRecordingPeriod {

            @Test
            @DisplayName("참여 가능으로 표시하고 현재 동의 안내 버전을 반환한다")
            void shouldReturnJoinableResult() {
                // given
                givenGatheringCreatedAt(LocalDateTime.now().minusHours(1));

                // when
                GatheringResult result = gatheringQueryService.getByJoinToken(JOIN_TOKEN);

                // then
                assertThat(result.name()).isEqualTo(GatheringFixture.FRIDAY_DINNER.getName());
                assertThat(result.joinable()).isTrue();
                assertThat(result.consentVersion()).isEqualTo(GatheringPolicy.CONSENT_VERSION);
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 지난 경우")
        class WhenRecordingPeriodPassed {

            @Test
            @DisplayName("참여 불가로 표시한다")
            void shouldReturnNotJoinableResult() {
                // given
                givenGatheringCreatedAt(LocalDateTime.now().minusHours(49));

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
