package com.symphonia.pairing.application.listener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import com.symphonia.pairing.application.event.PairingFeedbackSubmittedEvent;
import com.symphonia.pairing.application.event.PairingRecommendedEvent;
import com.symphonia.pairing.application.recorder.PairingMetricRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

// 커밋 시점에 실패하는 요청은 세지 않도록, 커밋이 끝난 뒤에 센다. 참여자 피드백은 모임 쪽 트랜잭션이 커밋된 뒤에 센다.
@Component
@RequiredArgsConstructor
public class PairingMetricListener {
    private final PairingMetricRecorder pairingMetricRecorder;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(PairingRecommendedEvent event) {
        pairingMetricRecorder.recordRecommended();
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(PairingFeedbackSubmittedEvent event) {
        pairingMetricRecorder.recordFeedbackSubmitted(event.type());
    }
}
