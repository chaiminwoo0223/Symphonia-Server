package com.symphonia.pairing.application.recorder;

import com.symphonia.pairing.application.event.PairingFeedbackSubmittedEvent.SubmitterType;

// 페어링 흐름의 전환 지표를 건수로 남긴다. 개인정보와 식별자는 받지 않는다.
public interface PairingMetricRecorder {
    void recordRecommended();

    void recordFeedbackSubmitted(SubmitterType type);
}
