package com.symphonia.gathering.application.listener;

import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.event.GatheringOpenedEvent;
import com.symphonia.gathering.application.event.ParticipantJoinedEvent;
import com.symphonia.gathering.application.event.ParticipantJoinedEvent.JoinType;
import com.symphonia.gathering.application.event.SettlementRecordedEvent;
import com.symphonia.gathering.application.event.SharesSentEvent;
import com.symphonia.gathering.application.recorder.GatheringMetricRecorder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("GatheringMetricListener 단위 테스트")
class GatheringMetricListenerTest extends UnitTest {

    @InjectMocks private GatheringMetricListener gatheringMetricListener;

    @Mock private GatheringMetricRecorder gatheringMetricRecorder;

    @Nested
    @DisplayName("handle 메서드는")
    class Handle {

        @Test
        @DisplayName("모임을 연 이벤트를 받으면 모임 생성을 기록한다")
        void shouldRecordGatheringOpenedWhenGatheringOpenedEventReceived() {
            // given
            GatheringOpenedEvent event = new GatheringOpenedEvent();

            // when
            gatheringMetricListener.handle(event);

            // then
            then(gatheringMetricRecorder).should().recordGatheringOpened();
        }

        @Test
        @DisplayName("참여 이벤트를 받으면 참여 경로와 함께 참여를 기록한다")
        void shouldRecordParticipantJoinedWhenParticipantJoinedEventReceived() {
            // given
            ParticipantJoinedEvent event = ParticipantJoinedEvent.byHost();

            // when
            gatheringMetricListener.handle(event);

            // then
            then(gatheringMetricRecorder).should().recordParticipantJoined(JoinType.HOST);
        }

        @Test
        @DisplayName("정산 건을 기록한 이벤트를 받으면 정산 건 기록을 기록한다")
        void shouldRecordSettlementRecordedWhenSettlementRecordedEventReceived() {
            // given
            SettlementRecordedEvent event = new SettlementRecordedEvent();

            // when
            gatheringMetricListener.handle(event);

            // then
            then(gatheringMetricRecorder).should().recordSettlementRecorded();
        }

        @Test
        @DisplayName("보냄 이벤트를 받으면 \"보냈어요\" 요청을 기록한다")
        void shouldRecordSharesSentWhenSharesSentEventReceived() {
            // given
            SharesSentEvent event = new SharesSentEvent();

            // when
            gatheringMetricListener.handle(event);

            // then
            then(gatheringMetricRecorder).should().recordSharesSent();
        }
    }
}
