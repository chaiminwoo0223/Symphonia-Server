package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.symphonia.UnitTest;
import com.symphonia.gathering.domain.repository.NotificationScheduleRepository;
import com.symphonia.global.config.TimeConfig;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@DisplayName("NotificationQueryService 단위 테스트")
class NotificationQueryServiceTest extends UnitTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 28, 15, 0);

    private NotificationQueryService notificationQueryService;

    @Mock private NotificationScheduleRepository notificationScheduleRepository;

    @BeforeEach
    void setUp() {
        notificationQueryService =
                new NotificationQueryService(
                        notificationScheduleRepository,
                        Clock.fixed(
                                NOW.atZone(TimeConfig.SERVICE_ZONE).toInstant(),
                                TimeConfig.SERVICE_ZONE));
    }

    @Nested
    @DisplayName("getDueGatheringIds 메서드는")
    class GetDueGatheringIds {

        @Test
        @DisplayName("현재 시각과 한 번에 시작할 모임 수 100으로 예약 시각이 지난 모임 id를 찾아 반환한다")
        void shouldReturnDueGatheringIdsWithNowAndLimit() {
            // given
            given(notificationScheduleRepository.findDueGatheringIds(NOW, 100))
                    .willReturn(List.of(10L, 11L));

            // when
            List<Long> result = notificationQueryService.getDueGatheringIds();

            // then
            assertThat(result).containsExactly(10L, 11L);
        }
    }
}
