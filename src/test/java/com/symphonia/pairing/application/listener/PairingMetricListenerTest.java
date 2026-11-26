package com.symphonia.pairing.application.listener;

import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.pairing.application.event.PairingFeedbackSubmittedEvent;
import com.symphonia.pairing.application.event.PairingFeedbackSubmittedEvent.SubmitterType;
import com.symphonia.pairing.application.event.PairingRecommendedEvent;
import com.symphonia.pairing.application.recorder.PairingMetricRecorder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("PairingMetricListener 단위 테스트")
class PairingMetricListenerTest extends UnitTest {

    @InjectMocks private PairingMetricListener pairingMetricListener;

    @Mock private PairingMetricRecorder pairingMetricRecorder;

    @Nested
    @DisplayName("handle 메서드는")
    class Handle {

        @Test
        @DisplayName("추천한 이벤트를 받으면 추천 요청을 기록한다")
        void shouldRecordRecommendedWhenPairingRecommendedEventReceived() {
            // given
            PairingRecommendedEvent event = new PairingRecommendedEvent();

            // when
            pairingMetricListener.handle(event);

            // then
            then(pairingMetricRecorder).should().recordRecommended();
        }

        @Test
        @DisplayName("피드백을 제출한 이벤트를 받으면 제출 경로와 함께 피드백 제출을 기록한다")
        void shouldRecordFeedbackSubmittedWhenPairingFeedbackSubmittedEventReceived() {
            // given
            PairingFeedbackSubmittedEvent event = PairingFeedbackSubmittedEvent.byParticipant();

            // when
            pairingMetricListener.handle(event);

            // then
            then(pairingMetricRecorder).should().recordFeedbackSubmitted(SubmitterType.PARTICIPANT);
        }
    }
}
