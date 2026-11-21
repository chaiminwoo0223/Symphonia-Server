package com.symphonia.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.result.OngoingGatheringsResult;
import com.symphonia.gathering.application.usecase.GetOngoingGatheringsUseCase;
import com.symphonia.member.application.dto.result.WithdrawalCheckResult;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("WithdrawalCheckService 단위 테스트")
class WithdrawalCheckServiceTest extends UnitTest {

    private static final Long MEMBER_ID = 1L;
    private static final LocalDateTime LAST_DELETES_AT = LocalDateTime.of(2026, 11, 30, 18, 0);

    @InjectMocks private WithdrawalCheckService withdrawalCheckService;

    @Mock private GetOngoingGatheringsUseCase getOngoingGatheringsUseCase;

    @Nested
    @DisplayName("check 메서드는")
    class Check {

        @Nested
        @DisplayName("진행 중인 모임이 있는 경우")
        class WhenOngoingGatheringsExist {

            @Test
            @DisplayName("모임 수와 가장 늦은 파기 예정 시각을 반환한다")
            void shouldReturnCountAndLastDeletesAt() {
                // given
                given(getOngoingGatheringsUseCase.getOngoing(MEMBER_ID))
                        .willReturn(new OngoingGatheringsResult(2, LAST_DELETES_AT));

                // when
                WithdrawalCheckResult result = withdrawalCheckService.check(MEMBER_ID);

                // then
                assertThat(result.ongoingGatheringCount()).isEqualTo(2);
                assertThat(result.lastGatheringDeletesAt()).isEqualTo(LAST_DELETES_AT);
            }
        }

        @Nested
        @DisplayName("진행 중인 모임이 없는 경우")
        class WhenNoOngoingGatherings {

            @Test
            @DisplayName("모임 수 0과 파기 예정 시각 없음을 반환한다")
            void shouldReturnZeroCountAndNoDeletesAt() {
                // given
                given(getOngoingGatheringsUseCase.getOngoing(MEMBER_ID))
                        .willReturn(new OngoingGatheringsResult(0, null));

                // when
                WithdrawalCheckResult result = withdrawalCheckService.check(MEMBER_ID);

                // then
                assertThat(result.ongoingGatheringCount()).isZero();
                assertThat(result.lastGatheringDeletesAt()).isNull();
            }
        }
    }
}
